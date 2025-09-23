package tokyo.isseikuzumaki.atvremote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceConnectionState
import com.shepeliev.webrtckmp.IceServer
import com.shepeliev.webrtckmp.IceTransportPolicy
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.SignalingState
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onIceConnectionStateChange
import com.shepeliev.webrtckmp.onSignalingStateChange
import com.shepeliev.webrtckmp.onTrack
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.path
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import tokyo.isseikuzumaki.atvremote.asDomain
import tokyo.isseikuzumaki.atvremote.shared.AdbCommand
import tokyo.isseikuzumaki.atvremote.shared.DeviceId
import tokyo.isseikuzumaki.atvremote.shared.IAtvControlService
import tokyo.isseikuzumaki.atvremote.shared.ISessionService
import tokyo.isseikuzumaki.atvremote.shared.ISignalingService
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SERVER_DOMAIN
import tokyo.isseikuzumaki.atvremote.shared.SERVER_PORT
import tokyo.isseikuzumaki.atvremote.shared.Sdp
import tokyo.isseikuzumaki.atvremote.shared.SessionID
import tokyo.isseikuzumaki.atvremote.shared.SignalingAnswer
import tokyo.isseikuzumaki.atvremote.shared.SignalingCandidate
import tokyo.isseikuzumaki.atvremote.shared.SignalingOffer
import kotlin.reflect.typeOf


class AppViewModel : ViewModel() {
    val rpcClient by lazy {
        HttpClient {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                retryIf { _, httpResponse ->
                    httpResponse.status.value >= 500
                }
                delayMillis { retry -> retry * 1000L } // 1秒、2秒、3秒
            }
            installKrpc {
                waitForServices = true
            }
        }.rpc {
            url {
                host = SERVER_DOMAIN
                port = SERVER_PORT
                path("rpc")
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }
    val adb by lazy { rpcClient.withService<IAtvControlService>() }
    val signaling by lazy { rpcClient.withService<ISignalingService>() }
    val session by lazy { rpcClient.withService<ISessionService>() }

    val adbDevices = adb.adbDevices()
    val waitingList = session.waitingSessions()

    private val _activeVideo = MutableStateFlow<VideoTrack?>(null)
    val activeVideo = _activeVideo.asStateFlow()

    private val _videoSession = MutableStateFlow<SessionID?>(null)
    val videoSession = _videoSession.asStateFlow()

    private val _adbDevice = MutableStateFlow<DeviceId?>(null)
    val adbDevice = _adbDevice.asStateFlow()

    // WebRTC自動再接続用の状態管理
    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting = _isReconnecting.asStateFlow()

    fun select(adbDevice: DeviceId) {
        _adbDevice.value = adbDevice
    }

    fun select(sessionId: SessionID) {
        Logger.d(TAG, "selectDevice: $sessionId")

        if (_videoSession.value == sessionId) {
            Logger.d(TAG, "Device $sessionId is already active, ignoring select")
            return
        }

        if (_videoSession.value != null) {
            Logger.todo(TAG, "Another device is active, closing existing connection")
            // TODO: Close existing connection
        }

        _videoSession.value = sessionId
        openVideo()
    }

    fun openVideo() {
        Logger.d(TAG, "openVideo")
        val device = _videoSession.value ?: run {
            Logger.d(TAG, "No active device, cannot open video")
            return
        }
        val candidates = mutableListOf<IceCandidateData>()

        // Configure STUN servers for NAT traversal
        val iceServers = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302"
        ).map {
            IceServer(urls = listOf(it))
        }

        val configuration = RtcConfiguration(
            iceServers = iceServers,
            iceTransportPolicy = IceTransportPolicy.All
        )

        val conn = PeerConnection(configuration).apply {
            onIceCandidate.onEach { candidate ->
                Logger.d(
                    TAG, "onIceCandidate: $candidate"
                )
                candidates.add(candidate.asDomain())
                signaling.putIceCandidates(
                    SignalingCandidate(
                        sessionID = device,
                        candidates = listOf(
                            candidate.asDomain()
                        )
                    )
                ).collect()
            }.launchIn(viewModelScope)
            onSignalingStateChange.onEach { signalingState ->
                Logger.d(TAG, "onSignalingStateChange: $signalingState")
                when (signalingState) {
                    SignalingState.Stable -> {
                        Logger.d(TAG, "SignalingState.Stable")
                    }

                    SignalingState.HaveLocalOffer -> {
                        Logger.d(TAG, "SignalingState.HaveLocalOffer")
                    }

                    SignalingState.HaveLocalPranswer -> {
                        Logger.d(TAG, "SignalingState.HaveLocalPranswer")
                    }

                    SignalingState.HaveRemoteOffer -> {
                        Logger.d(TAG, "SignalingState.HaveRemoteOffer")
                    }

                    SignalingState.HaveRemotePranswer -> {
                        Logger.d(TAG, "SignalingState.HaveRemotePranswer")
                    }

                    SignalingState.Closed -> {
                        Logger.d(TAG, "SignalingState.Closed")
                    }
                }
            }.launchIn(viewModelScope)
            onIceConnectionStateChange.onEach { iceConnectionState ->
                Logger.d(TAG, "onIceConnectionStateChange: $iceConnectionState")
                when (iceConnectionState) {
                    IceConnectionState.New -> {
                        Logger.todo(TAG, "IceConnectionState.New")
                    }

                    IceConnectionState.Checking -> {
                        Logger.todo(TAG, "IceConnectionState.Checking")
                    }

                    IceConnectionState.Connected -> {
                        Logger.todo(TAG, "IceConnectionState.Connected")
                    }

                    IceConnectionState.Completed -> {
                        Logger.todo(TAG, "IceConnectionState.Completed")
                    }

                    IceConnectionState.Failed -> {
                        Logger.e(TAG, "WebRTC connection failed, attempting reconnect...")
                        viewModelScope.launch {
                            delay(3000) // 3秒待機
                            reopenVideo()
                        }
                    }

                    IceConnectionState.Disconnected -> {
                        Logger.e(TAG, "WebRTC disconnected, attempting reconnect...")
                        viewModelScope.launch {
                            delay(1000) // 1秒待機
                            reopenVideo()
                        }
                    }

                    IceConnectionState.Closed -> {
                        Logger.todo(TAG, "IceConnectionState.Closed")
                    }

                    IceConnectionState.Count -> {
                        Logger.todo(TAG, "IceConnectionState.Count")
                    }
                }
            }.launchIn(viewModelScope)
            onTrack.onEach { event ->
                Logger.d(TAG, "onTrack: $event")
                (event.track as? VideoTrack)
                    ?.let { _activeVideo.value = it }
                    ?: run {
                        Logger.d(TAG, "Not a video track, ignoring")
                    }
            }.launchIn(viewModelScope)
        }

        viewModelScope.launch {
            val offer = conn.createOffer(
                OfferAnswerOptions(
                    offerToReceiveAudio = false,
                    offerToReceiveVideo = true,
                )
            )
            conn.setLocalDescription(offer)

            val answer = signaling.offer(
                SignalingOffer(
                    sessionID = device,
                    sdp = Sdp(offer.sdp)
                )
            ).collect { answer ->
                Logger.d(TAG, "offer collected: $answer")

                when (answer) {
                    is SignalingAnswer.Answer -> {
                        Logger.d(TAG, "Received full answer with SDP")
                        conn.setRemoteDescription(
                            SessionDescription(
                                type = SessionDescriptionType.Answer,
                                sdp = answer.sdp.value
                            )
                        )
                    }

                    is SignalingAnswer.Candidate -> {
                        Logger.d(TAG, "Received ICE candidate only")
                        conn.addIceCandidate(answer.candidate.asLibrary())
                    }
                }
            }
        }
    }

    /**
     * WebRTC接続の再接続処理
     * 重複実行を防止し、既存の接続をクリアしてから新しい接続を開始する
     */
    private fun reopenVideo() {
        if (_isReconnecting.value) {
            Logger.d(TAG, "Already reconnecting, skipping duplicate reconnect attempt")
            return
        }

        _isReconnecting.value = true
        Logger.d(TAG, "Starting WebRTC reconnection process")

        try {
            // 現在のビデオ接続をクリア
            _activeVideo.value = null
            // 既存のopenVideo()関数を再利用して新しい接続を開始
            openVideo()
        } finally {
            _isReconnecting.value = false
        }
    }

    fun sendAdbCommand(command: String) = flow {
        val device = _adbDevice.value ?: run {
            Logger.d(TAG, "No active device, cannot send ADB command")
            emit(Result.failure<Unit>(IllegalStateException("No active device")))
            return@flow
        }

        Logger.d(TAG, "Sending ADB command: $command to device: $device")

        adb.sendAdbCommand(
            deviceId = device,
            command = AdbCommand(command)
        ).onEach { result ->
            Logger.d(TAG, "ADB command result: $result")
            emit(result)
        }.first()
    }

    fun takeScreenshot() = flow {
        val device = _adbDevice.value ?: run {
            Logger.d(TAG, "No active device, cannot take screenshot")
            return@flow
        }

        try {
            adb.takeScreenshot(device).onEach { result ->
                Logger.d(TAG, "Screenshot result: success=${result.isSuccess}, size=${result.imageData.size} bytes")
                if (result.isSuccess) {
                    emit(Result.success(result))
                } else {
                    emit(Result.failure(Exception("Failed to take screenshot")))
                }
            }.first()
        } catch (e: Exception) {
            Logger.e(TAG, "Error taking screenshot: ${e.message}")
            emit(Result.failure(e))
        }
    }

private fun IceCandidateData.asLibrary(): IceCandidate {
    val domain = this
    return IceCandidate(
        sdpMid = domain.sdpMid,
        sdpMLineIndex = domain.sdpMLineIndex,
        candidate = domain.candidate
    )
}

    companion object {
        private const val TAG = "AppViewModel"
    }
}

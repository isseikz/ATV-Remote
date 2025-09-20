package tokyo.isseikuzumaki.atvremote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shepeliev.webrtckmp.IceConnectionState
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.SignalingState
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onIceConnectionStateChange
import com.shepeliev.webrtckmp.onSignalingStateChange
import com.shepeliev.webrtckmp.onTrack
import io.ktor.client.HttpClient
import io.ktor.http.path
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import tokyo.isseikuzumaki.atvremote.shared.AtvControlService
import tokyo.isseikuzumaki.atvremote.shared.DeviceId
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.ScreenshotResult
import tokyo.isseikuzumaki.atvremote.shared.SERVER_DOMAIN
import tokyo.isseikuzumaki.atvremote.shared.SERVER_PORT
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer


class AppViewModel : ViewModel() {
    val rpcClient by lazy {
        HttpClient {
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
    val service by lazy {
        rpcClient.withService<AtvControlService>()
    }

    private val _activeVideo = MutableStateFlow<VideoTrack?>(null)
    val activeVideo = _activeVideo.asStateFlow()

    private val _activeDevice = MutableStateFlow<DeviceId?>(null)
    val activeDevice = _activeDevice.asStateFlow()

    fun observeDevices() = flow {

        while (true) {
            service.adbDevices().onEach { devices ->
                Logger.d(TAG, "adbDevices: $devices")
                emit(devices)
            }.first()

            delay(5000)
        }
    }

    fun selectDevice(deviceId: DeviceId) {
        Logger.d(TAG, "selectDevice: $deviceId")

        if (_activeDevice.value == deviceId) {
            Logger.d(TAG, "Device $deviceId is already active, ignoring select")
            return
        }

        if (_activeDevice.value != null) {
            Logger.todo(TAG, "Another device is active, closing existing connection")
            // TODO: Close existing connection
        }

        _activeDevice.value = deviceId
    }

    fun openVideo() {
        Logger.d(TAG, "openVideo")
        val device = _activeDevice.value ?: run {
            Logger.d(TAG, "No active device, cannot open video")
            return
        }
        val candidates = mutableListOf<IceCandidateData>()
        val conn = PeerConnection().apply {
            onIceCandidate.onEach { candidate ->
                Logger.d(
                    TAG, "onIceCan" +
                            "didate: $candidate"
                )
                candidates.add(candidate.asDomain())
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
                        Logger.todo(TAG, "IceConnectionState.Failed")
                    }

                    IceConnectionState.Disconnected -> {
                        Logger.todo(TAG, "IceConnectionState.Disconnected")
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

            val answer = service.sendSdpOffer(
                deviceId = device,
                SdpOffer(
                    deviceId = device,
                    sdp = offer.sdp,
                )
            ).first()

            conn.setRemoteDescription(
                SessionDescription(
                    type = SessionDescriptionType.Answer,
                    sdp = answer.sdp
                )
            )
//            Logger.d(TAG, "Exchanging ICE candidates with ${candidates.size} candidates")
//
            service.sendIceCandidate(device, candidates.first()).first()
        }
    }

    fun sendAdbCommand(command: String) = flow {
        val device = _activeDevice.value ?: run {
            Logger.d(TAG, "No active device, cannot send ADB command")
            emit(Result.failure<Unit>(IllegalStateException("No active device")))
            return@flow
        }

        Logger.d(TAG, "Sending ADB command: $command to device: $device")

        service.sendAdbCommand(
            deviceId = device,
            command = AdbCommand(command)
        ).onEach { result ->
            Logger.d(TAG, "ADB command result: $result")
            emit(result)
        }.first()
    }

    fun takeScreenshot() = flow {
        val device = _activeDevice.value ?: run {
            Logger.d(TAG, "No active device, cannot take screenshot")
            emit(Result.failure<ScreenshotResult>(IllegalStateException("No active device")))
            return@flow
        }

        Logger.d(TAG, "Taking screenshot for device: $device")

        try {
            service.takeScreenshot(device).onEach { result ->
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

    companion object {
        private const val TAG = "AppViewModel"
    }
}

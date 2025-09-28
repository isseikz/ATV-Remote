package tokyo.isseikuzumaki.signalinglib.client

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tokyo.isseikuzumaki.signalinglib.shared.Logger
import tokyo.isseikuzumaki.signalinglib.shared.Sdp
import tokyo.isseikuzumaki.signalinglib.shared.SessionID
import tokyo.isseikuzumaki.signalinglib.shared.SignalingAnswer
import tokyo.isseikuzumaki.signalinglib.shared.SignalingCandidate
import tokyo.isseikuzumaki.signalinglib.shared.SignalingOffer

/**
 * WebRTC接続のラッパークラス
 * WebRTC-KMPライブラリを使用してWebRTC接続を管理する
 */
class WebRTCWrapper(
    private val coroutineScope: CoroutineScope,
    private val signalingClient: SignalingClient,
    private val onVideoTrack: (VideoTrack) -> Unit = {},
    private val onConnectionFailed: () -> Unit = {},
    private val onConnectionDisconnected: () -> Unit = {}
) {
    companion object {
        private const val TAG = "WebRTCWrapper"
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var currentPeerConnection: PeerConnection? = null

    /**
     * WebRTC接続を開始する
     */
    suspend fun connect(sessionId: SessionID) {
        Logger.d(TAG, "Starting WebRTC connection for session: $sessionId")

        // 既存の接続をクリーンアップ
        closeConnection()

        val candidates = mutableListOf<tokyo.isseikuzumaki.signalinglib.shared.IceCandidateData>()

        // STUN servers for NAT traversal
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

        val peerConnection = PeerConnection(configuration).apply {
            currentPeerConnection = this

            // ICE Candidate event handler
            onIceCandidate.onEach { candidate ->
                Logger.d(TAG, "onIceCandidate: $candidate")
                candidates.add(candidate.asDomain())
                signalingClient.signalingService.putIceCandidates(
                    SignalingCandidate(
                        sessionID = sessionId,
                        candidates = listOf(candidate.asDomain())
                    )
                ).collect()
            }.launchIn(coroutineScope)

            // Signaling state change handler
            onSignalingStateChange.onEach { signalingState ->
                Logger.d(TAG, "onSignalingStateChange: $signalingState")
                when (signalingState) {
                    SignalingState.Stable -> Logger.d(TAG, "SignalingState.Stable")
                    SignalingState.HaveLocalOffer -> Logger.d(TAG, "SignalingState.HaveLocalOffer")
                    SignalingState.HaveLocalPranswer -> Logger.d(TAG, "SignalingState.HaveLocalPranswer")
                    SignalingState.HaveRemoteOffer -> Logger.d(TAG, "SignalingState.HaveRemoteOffer")
                    SignalingState.HaveRemotePranswer -> Logger.d(TAG, "SignalingState.HaveRemotePranswer")
                    SignalingState.Closed -> Logger.d(TAG, "SignalingState.Closed")
                }
            }.launchIn(coroutineScope)

            // ICE connection state change handler
            onIceConnectionStateChange.onEach { iceConnectionState ->
                Logger.d(TAG, "onIceConnectionStateChange: $iceConnectionState")
                when (iceConnectionState) {
                    IceConnectionState.New -> Logger.d(TAG, "IceConnectionState.New")
                    IceConnectionState.Checking -> Logger.d(TAG, "IceConnectionState.Checking")
                    IceConnectionState.Connected -> {
                        Logger.d(TAG, "IceConnectionState.Connected")
                        _isConnected.value = true
                    }
                    IceConnectionState.Completed -> {
                        Logger.d(TAG, "IceConnectionState.Completed")
                        _isConnected.value = true
                    }
                    IceConnectionState.Failed -> {
                        Logger.e(TAG, "WebRTC connection failed")
                        _isConnected.value = false
                        onConnectionFailed()
                    }
                    IceConnectionState.Disconnected -> {
                        Logger.e(TAG, "WebRTC disconnected")
                        _isConnected.value = false
                        onConnectionDisconnected()
                    }
                    IceConnectionState.Closed -> {
                        Logger.d(TAG, "IceConnectionState.Closed")
                        _isConnected.value = false
                    }
                    IceConnectionState.Count -> Logger.d(TAG, "IceConnectionState.Count")
                }
            }.launchIn(coroutineScope)

            // Track event handler
            onTrack.onEach { event ->
                Logger.d(TAG, "onTrack: $event")
                (event.track as? VideoTrack)?.let { videoTrack ->
                    onVideoTrack(videoTrack)
                } ?: run {
                    Logger.d(TAG, "Not a video track, ignoring")
                }
            }.launchIn(coroutineScope)
        }

        // Create offer and start signaling process
        val offer = peerConnection.createOffer(
            OfferAnswerOptions(
                offerToReceiveAudio = false,
                offerToReceiveVideo = true,
            )
        )
        peerConnection.setLocalDescription(offer)

        // Send offer to signaling server
        signalingClient.signalingService.offer(
            SignalingOffer(
                sessionID = sessionId,
                sdp = Sdp(offer.sdp)
            )
        ).collect { answer ->
            Logger.d(TAG, "Received signaling answer: $answer")

            when (answer) {
                is SignalingAnswer.Answer -> {
                    Logger.d(TAG, "Received full answer with SDP")
                    peerConnection.setRemoteDescription(
                        SessionDescription(
                            type = SessionDescriptionType.Answer,
                            sdp = answer.sdp.value
                        )
                    )
                }

                is SignalingAnswer.Candidate -> {
                    Logger.d(TAG, "Received ICE candidate only")
                    peerConnection.addIceCandidate(answer.candidate.asLibrary())
                }
            }
        }
    }

    /**
     * WebRTC接続を閉じる
     */
    fun closeConnection() {
        Logger.d(TAG, "Closing WebRTC connection")

        _isConnected.value = false

        currentPeerConnection?.let { conn ->
            try {
                conn.close()
                Logger.d(TAG, "PeerConnection closed successfully")
            } catch (e: Exception) {
                Logger.e(TAG, "Error closing PeerConnection: ${e.message}")
            }
        }
        currentPeerConnection = null
    }
}
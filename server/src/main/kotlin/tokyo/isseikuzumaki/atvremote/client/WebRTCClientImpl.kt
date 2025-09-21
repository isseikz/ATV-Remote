package tokyo.isseikuzumaki.atvremote.client

import dev.onvoid.webrtc.CreateSessionDescriptionObserver
import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.PeerConnectionObserver
import dev.onvoid.webrtc.RTCAnswerOptions
import dev.onvoid.webrtc.RTCBundlePolicy
import dev.onvoid.webrtc.RTCConfiguration
import dev.onvoid.webrtc.RTCIceCandidate
import dev.onvoid.webrtc.RTCIceConnectionState
import dev.onvoid.webrtc.RTCIceGatheringState
import dev.onvoid.webrtc.RTCIceServer
import dev.onvoid.webrtc.RTCIceTransportPolicy
import dev.onvoid.webrtc.RTCPeerConnection
import dev.onvoid.webrtc.RTCPeerConnectionState
import dev.onvoid.webrtc.RTCRtcpMuxPolicy
import dev.onvoid.webrtc.RTCSdpType
import dev.onvoid.webrtc.RTCSessionDescription
import dev.onvoid.webrtc.RTCSignalingState
import dev.onvoid.webrtc.SetSessionDescriptionObserver
import dev.onvoid.webrtc.media.MediaDevices
import dev.onvoid.webrtc.media.video.VideoDevice
import dev.onvoid.webrtc.media.video.VideoDeviceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.atvremote.service.asDomain
import tokyo.isseikuzumaki.atvremote.service.asLibrary
import tokyo.isseikuzumaki.atvremote.service.signaling.ISignalingClient
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.Sdp
import tokyo.isseikuzumaki.atvremote.shared.SdpAnswer
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer
import java.util.UUID

/**
 * サーバーに組み込む WebRTC クライアントの実装
 * ビデオキャプチャごとに WebRTC クライアントを生成する
 */
class WebRTCClientImpl(
    private val videoCapture: VideoDevice,
    private val turnServerService: TurnServerService,
) : ISignalingClient {
    private val localConnection = MutableStateFlow<RTCPeerConnection?>(null)
    override fun handleOffer(offer: SdpOffer): Flow<SdpAnswer> = flow {
        Logger.d(TAG, "Handling offer: $offer")
        val localConfig = RTCConfiguration().apply {
            getSTUNServers().forEach { iceServers.add(it.asLibrary()) }
            getTURNServers().forEach { iceServers.add(it.asLibrary()) }

            bundlePolicy = RTCBundlePolicy.BALANCED
            iceTransportPolicy = RTCIceTransportPolicy.ALL
            rtcpMuxPolicy = RTCRtcpMuxPolicy.NEGOTIATE
        }

        val (localConn, candidates) = createPeerConnection(localConfig).first()

        val answer = localConn.answerToOffer(
            RTCSessionDescription(RTCSdpType.OFFER, offer.sdp.value)
        ).onEach {
            Logger.d(TAG, "Generated SDP answer: $it")
        }.first()

        emit(SdpAnswer(answer, emptyList()))

        Logger.d(TAG, "Generated answer with candidates: $candidates")

        localConnection.value = localConn

        emitAll(candidates.drop(1)
            .onEach {
                Logger.d(TAG, "CandidateCollector state: $it")
            }.map { return@map SdpAnswer(answer, it.map { it.asDomain() }) }
            .onEach {
                Logger.d(TAG, "Emitting additional candidates: $it")
            }
        )
    }

    override fun handlePutIceCandidates(candidate: List<IceCandidateData>): Flow<Unit> = flow {
        localConnection.first { it != null }
            .let { conn -> candidate.forEach { conn!!.addIceCandidate(it.asLibrary()) } } // null ではないことを保証する
    }

    private suspend fun getSTUNServers(): List<IceServer> = withContext(Dispatchers.IO) {
        // 将来的には別の STUN サーバーも使えるようにするかもしれないので、suspend 関数にしておく
        return@withContext listOf(
            IceServer(urls = listOf("stun:stun.l.google.com:19302")),
            IceServer(urls = listOf("stun:stun1.l.google.com:19302")),
            IceServer(urls = listOf("stun:stun2.l.google.com:19302")),
            IceServer(urls = listOf("stun:stun3.l.google.com:19302")),
            IceServer(urls = listOf("stun:stun4.l.google.com:19302")),
        )
    }

    private suspend fun getTURNServers(): List<IceServer> = withContext(Dispatchers.IO) {
        return@withContext turnServerService.getTurnCredentials().getOrThrow().iceServers
    }

    private fun createPeerConnection(config: RTCConfiguration) = flow {
        Logger.d(TAG, "Creating PeerConnection with config: $config")
        val factory = PeerConnectionFactory()

        val collector = CandidateCollector()
        val conn: RTCPeerConnection = factory.createPeerConnection(config, collector)

        Logger.d(TAG, "Waiting for ICE candidates to be gathered...")
        val candidates = collector.candidates

        // TODO 適切な stream ID の指定方法
        val streamIds: MutableList<String?> = ArrayList()
        streamIds.add(UUID.randomUUID().toString())

        val track = createVideoSource().let {
            factory.createVideoTrack("name", it)
        }
        conn.addTrack(track, streamIds)

        emit(conn to candidates)
    }

    /**
     * Offer を設定し、Answer を生成して返す
     */
    private fun RTCPeerConnection.answerToOffer(session: RTCSessionDescription) = callbackFlow {
        Logger.d(TAG, "Setting remote description: ${session.sdpType}")

        setRemoteDescription(session, object : SetSessionDescriptionObserver {
            override fun onSuccess() {
                createAnswer(RTCAnswerOptions(), object : CreateSessionDescriptionObserver {
                    override fun onSuccess(description: RTCSessionDescription?) {
                        if (description == null) {
                            throw IllegalStateException("Failed to create SDP answer: description is null")
                        }

                        setLocalDescription(description, object : SetSessionDescriptionObserver {
                            override fun onSuccess() {
                                Logger.d(TAG, "Set local description successfully")
                                trySend(description.asSdp())
                            }

                            override fun onFailure(error: String?) {
                                Logger.e(TAG, "Failed to set local description: $error")
                                throw IllegalStateException("Failed to set local description: $error")
                            }
                        })
                    }

                    override fun onFailure(error: String?) {
                        throw IllegalStateException("Failed to create SDP answer: $error")
                    }
                })
            }

            override fun onFailure(error: String?) {
                throw IllegalStateException("Failed to set remote description: $error")
            }
        })

        awaitClose { }
    }

    private fun IceServer.asLibrary(): RTCIceServer {
        val domain = this

        return RTCIceServer().apply {
            urls.addAll(domain.urls)
            domain.username?.let { username = it }
            domain.credential?.let { password = it }
        }
    }

    private fun RTCSessionDescription.asSdp(): Sdp {
        return Sdp(value = this.sdp)
    }

    private fun createVideoSource(): VideoDeviceSource {
        Logger.d(TAG, "Creating video source for device: ${videoCapture.name}")
        val videoSource = VideoDeviceSource().apply {
            val capabilities = MediaDevices.getVideoCaptureCapabilities(videoCapture).firstOrNull()
                ?: throw IllegalStateException("No capabilities found for video device: ${videoCapture.name}")
            setVideoCaptureDevice(videoCapture)
            setVideoCaptureCapability(capabilities)
        }
        videoSource.start()
        return videoSource
    }

    /**
     * ビデオキャプチャを選択して開始し、WebRTC の
     */
    class Factory(
        private val turnServerService: TurnServerService,
    ) {
        fun getDevices(): List<VideoDevice> {
            return MediaDevices.getVideoCaptureDevices()
        }

        fun createClient(videoDevice: VideoDevice): WebRTCClientImpl {
            return WebRTCClientImpl(videoDevice, turnServerService)
        }
    }

    /** ローカルの ICE Candidate 情報を収集し、一覧を返す
     *
     */
    class CandidateCollector() : PeerConnectionObserver {
        sealed class State {
            object Gathering : State()
            data class Complete(
                val candidates: List<IceCandidateData>,
            ) : State()
        }

        private val _state: MutableStateFlow<State> = MutableStateFlow(State.Gathering)
        val state: Flow<State> get() = _state

        private val _candidates = MutableStateFlow<List<RTCIceCandidate>>(emptyList())
        val candidates: Flow<List<RTCIceCandidate>> get() = _candidates

        override fun onIceCandidate(candidate: RTCIceCandidate) {
            Logger.d(TAG, "onIceCandidate: $candidate")
            _candidates.value = _candidates.value + candidate
        }

        override fun onIceConnectionChange(state: RTCIceConnectionState) {
            Logger.todo(TAG, "onIceConnectionChange: $state")
        }

        override fun onConnectionChange(state: RTCPeerConnectionState) {
            Logger.todo(TAG, "onConnectionChange: $state")
        }

        override fun onSignalingChange(state: RTCSignalingState) {
            Logger.todo(TAG, "onSignalingChange: $state")
        }

        override fun onIceGatheringChange(state: RTCIceGatheringState) {
            Logger.todo(TAG, "onIceGatheringChange: $state")

            when (state) {
                RTCIceGatheringState.COMPLETE -> {
                    Logger.d(TAG, "ICE gathering complete")
                    _state.value = State.Complete(_candidates.value.map { it.asDomain() })
                }

                RTCIceGatheringState.GATHERING -> {
                    Logger.d(TAG, "ICE gathering in progress")
                }

                RTCIceGatheringState.NEW -> {
                    Logger.d(TAG, "ICE gathering new")
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebRTCClientImpl"
    }
}
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
    private var videoSource: VideoDeviceSource? = null
    private var isCleanedUp = false
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

        val collector = CandidateCollector(this@WebRTCClientImpl)
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
        // 再接続時の状態リセット
        if (isCleanedUp) {
            Logger.d(TAG, "Resetting cleanup state for reconnection of device: ${videoCapture.name}")
            isCleanedUp = false
        }

        Logger.d(TAG, "Creating video source for device: ${videoCapture.name}")
        val source = VideoDeviceSource().apply {
            val capabilities = MediaDevices.getVideoCaptureCapabilities(videoCapture)
                ?: throw IllegalStateException("No capabilities found for video device: ${videoCapture.name}")
            setVideoCaptureDevice(videoCapture)
            capabilities.forEach {
                Logger.d(TAG, "Capability: $it")
                setVideoCaptureCapability(it)
            }
        }
        source.start()
        videoSource = source // 参照を保持
        return source
    }

    /**
     * WebRTCリソースのクリーンアップ処理
     * VideoSourceの停止とPeerConnectionの解放を行う
     */
    fun cleanup() {
        if (isCleanedUp) {
            Logger.d(TAG, "Cleanup already performed for device: ${videoCapture.name}, skipping")
            return
        }

        Logger.d(TAG, "Starting cleanup of WebRTC client resources for device: ${videoCapture.name}")
        isCleanedUp = true

        // 1. まずPeerConnectionを閉じる（VideoSourceへの参照を切断）
        localConnection.value?.let { conn ->
            try {
                Logger.d(TAG, "Closing PeerConnection...")
                conn.close()
                Logger.d(TAG, "PeerConnection closed successfully")
            } catch (e: Exception) {
                Logger.e(TAG, "Error closing PeerConnection: ${e.message}")
            }
        }
        localConnection.value = null

        // 2. 少し待機してからVideoSourceを停止（JNI呼び出しの競合を避ける）
        try {
            Thread.sleep(100) // 100ms待機
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // 3. VideoSourceを停止
        videoSource?.let { source ->
            try {
                Logger.d(TAG, "Stopping VideoSource...")
                source.stop()
                Logger.d(TAG, "VideoSource stopped successfully")
            } catch (e: Exception) {
                Logger.e(TAG, "Error stopping VideoSource: ${e.message}")
                // VideoSource停止でエラーが発生してもアプリケーションを継続
            }
        }
        videoSource = null

        Logger.d(TAG, "WebRTC client cleanup completed for device: ${videoCapture.name}")
    }

    /**
     * VideoSourceがアクティブかどうかを判定
     * @return VideoSourceが作成され、まだクリーンアップされていない場合はtrue
     */
    fun isVideoSourceActive(): Boolean = videoSource != null && !isCleanedUp

    /**
     * 再接続可能かどうかを判定
     * @return クリーンアップ済みでも再接続可能な場合はtrue
     */
    fun canReconnect(): Boolean = true // WebRTCClientImplは常に再接続可能

    /**
     * 明示的な再接続準備
     * クリーンアップ後の再接続で呼び出される
     */
    fun prepareForReconnection() {
        Logger.d(TAG, "Preparing for reconnection of device: ${videoCapture.name}")
        if (isCleanedUp) {
            isCleanedUp = false
            Logger.d(TAG, "Reset cleanup state for device: ${videoCapture.name}")
        }
    }

    /**
     * ビデオキャプチャを選択して開始し、WebRTC の
     */
    class Factory(
        private val turnServerService: TurnServerService,
    ) {
        private val clientRegistry = mutableMapOf<String, WebRTCClientImpl>()

        fun getDevices(): List<VideoDevice> {
            return MediaDevices.getVideoCaptureDevices()
        }

        fun createClient(videoDevice: VideoDevice): WebRTCClientImpl {
            return WebRTCClientImpl(videoDevice, turnServerService)
        }

        /**
         * デバイス名に基づいてクライアントを取得または作成
         * 既存のクライアントがある場合は再利用（再接続準備付き）
         *
         * 使用例:
         * ```
         * // カメラ接続時
         * val client = factory.getOrCreateClient(videoDevice)
         *
         * // カメラ切断時
         * factory.cleanupClient(videoDevice.name)
         *
         * // カメラ再接続時（同じクライアントが再利用される）
         * val sameClient = factory.getOrCreateClient(videoDevice)
         * ```
         */
        fun getOrCreateClient(videoDevice: VideoDevice): WebRTCClientImpl {
            val deviceKey = videoDevice.name
            val existingClient = clientRegistry[deviceKey]

            return if (existingClient != null && existingClient.canReconnect()) {
                Logger.d("WebRTCClientFactory", "Reusing existing client for device: ${videoDevice.name}")
                existingClient.prepareForReconnection()
                existingClient
            } else {
                Logger.d("WebRTCClientFactory", "Creating new client for device: ${videoDevice.name}")
                val newClient = WebRTCClientImpl(videoDevice, turnServerService)
                clientRegistry[deviceKey] = newClient
                newClient
            }
        }

        /**
         * 使用されなくなったクライアントをクリーンアップ
         */
        fun cleanupClient(deviceName: String) {
            clientRegistry[deviceName]?.let { client ->
                Logger.d("WebRTCClientFactory", "Cleaning up client for device: $deviceName")
                client.cleanup()
                // クライアントはレジストリに残す（再接続時の再利用のため）
            }
        }

        /**
         * 全クライアントをクリーンアップ（シャットダウン時用）
         */
        fun cleanupAllClients() {
            Logger.d("WebRTCClientFactory", "Cleaning up all clients")
            clientRegistry.values.forEach { it.cleanup() }
            clientRegistry.clear()
        }
    }

    /** ローカルの ICE Candidate 情報を収集し、一覧を返す
     *
     */
    class CandidateCollector(
        private val client: WebRTCClientImpl
    ) : PeerConnectionObserver {
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
            Logger.d(TAG, "onIceConnectionChange: $state")
            when (state) {
                RTCIceConnectionState.FAILED -> {
                    Logger.e(TAG, "Server-side ICE connection failed, cleaning up resources")
                    client.cleanup()
                }
                RTCIceConnectionState.DISCONNECTED -> {
                    Logger.d(TAG, "Server-side ICE connection disconnected (may recover automatically)")
                    // DISCONNECTEDは一時的な状態の場合があるため、cleanup()は呼ばない
                }
                RTCIceConnectionState.CLOSED -> {
                    Logger.d(TAG, "Server-side ICE connection closed")
                    // CLOSEDは既にcleanup()が実行された結果の可能性が高いため、重複防止のため呼ばない
                }
                else -> {
                    // その他の状態はログのみ
                    Logger.d(TAG, "ICE connection state: $state")
                }
            }
        }

        override fun onConnectionChange(state: RTCPeerConnectionState) {
            Logger.d(TAG, "onConnectionChange: $state")
            when (state) {
                RTCPeerConnectionState.FAILED -> {
                    Logger.e(TAG, "Server-side peer connection failed, cleaning up")
                    client.cleanup()
                }
                RTCPeerConnectionState.DISCONNECTED -> {
                    Logger.d(TAG, "Server-side peer connection disconnected (may recover automatically)")
                    // DISCONNECTEDは一時的な状態の場合があるため、cleanup()は呼ばない
                }
                RTCPeerConnectionState.CLOSED -> {
                    Logger.d(TAG, "Server-side peer connection closed")
                    // CLOSEDは既にcleanup()が実行された結果の可能性が高いため、重複防止のため呼ばない
                }
                else -> {
                    // その他の状態はログのみ
                    Logger.d(TAG, "Peer connection state: $state")
                }
            }
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
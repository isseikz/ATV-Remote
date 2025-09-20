package tokyo.isseikuzumaki.atvremote.service

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
import dev.onvoid.webrtc.media.video.VideoDeviceSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tokyo.isseikuzumaki.atvremote.shared.DeviceId
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.Logger
import tokyo.isseikuzumaki.atvremote.shared.SdpAnswer
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class WebRTCSignalingManager {
    private val mutex = Mutex()
    private val session = ConcurrentHashMap<DeviceId, SessionData>()

    data class SessionData(
        val localConn: RTCPeerConnection?,
        val localCandidate: RTCIceCandidate?,
        val remoteSdp: String?,
    )

    /**
     * Offer を受け取り、自身の PeerConnection から Answer を生成して返す
     *
     * @param deviceId クライアントのデバイスID
     * @param offer クライアントから送信された SDP Offer
     *
     * @return クライアントに返す SDP Answer を含む Flow
     *
     * @throws IllegalArgumentException 引数が不正な場合
     * @throws InternalError Answer の生成に失敗した場合
     */
    fun handleOffer(deviceId: DeviceId, offer: SdpOffer) = flow {
        Logger.d(TAG, "Handling SDP Offer for device ${deviceId.value}")

        val localConfig = RTCConfiguration().apply {
            iceServers.add(RTCIceServer().apply {
                urls.add("stun:stun.l.google.com:19302")
                urls.add("stun:stun4.l.google.com:19302");
            })
            bundlePolicy = RTCBundlePolicy.MAX_BUNDLE
            iceTransportPolicy = RTCIceTransportPolicy.ALL
            rtcpMuxPolicy = RTCRtcpMuxPolicy.NEGOTIATE
        }

        Logger.d(TAG, "Creating PeerConnection for device ${deviceId.value}")
        val localConn = createPeerConnection(deviceId, localConfig).first().getOrThrow()

        val answer = localConn.answerToOffer(
            RTCSessionDescription(RTCSdpType.OFFER, offer.sdp)
        ).first().getOrThrow()

        session.getOrDefault(deviceId, SessionData(null, null, null))
            .copy(localConn = localConn, remoteSdp = offer.sdp)
            .let { session.put(deviceId, it) }

        Logger.d(TAG, "Generated SDP Answer for device ${deviceId.value}")
        emit(Result.success(SdpAnswer(deviceId, answer.sdp)))
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createPeerConnection(deviceId: DeviceId, config: RTCConfiguration) = callbackFlow {
        val factory = try {
            PeerConnectionFactory()
        } catch (e: RuntimeException) {
            Logger.e(TAG, "Failed to create PeerConnectionFactory: ${e.message}", e)
            trySend(Result.failure(Error("Failed to create PeerConnectionFactory: ${e.message}", e)))
            return@callbackFlow
        }

        val videoSource = VideoDeviceSource().apply {
            val camera = MediaDevices.getVideoCaptureDevices().firstOrNull() ?: run {
                Logger.e(TAG, "No video capture devices found")
                trySend(Result.failure(Error("No video capture devices found")))
                return@callbackFlow
            }
            val capabilities = MediaDevices.getVideoCaptureCapabilities(camera).firstOrNull() ?: run {
                Logger.e(TAG, "No video capture capabilities found for device: ${camera.name}")
                trySend(Result.failure(Error("No video capture capabilities found for device: ${camera.name}")))
                return@callbackFlow
            }
            setVideoCaptureDevice(camera)
            setVideoCaptureCapability(capabilities)
        }
        videoSource.start()
        val videoTrack = factory.createVideoTrack(deviceId.value, videoSource)

        val conn: RTCPeerConnection = factory.createPeerConnection(config, object : PeerConnectionObserver {
            override fun onIceCandidate(candidate: RTCIceCandidate) {
                Logger.d(TAG, "onIceCandidate: $candidate")
                session.getOrDefault(deviceId, SessionData(null, null, null))
                    .copy(localCandidate = candidate)
                    .let { session.put(deviceId, it) }
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
            }
        })

        val streamIds: MutableList<String?> = ArrayList<String?>()
        streamIds.add(Uuid.random().toString())
        conn.addTrack(videoTrack, streamIds)

        trySend(Result.success(conn))

        awaitClose {  }
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
                            trySend(Result.failure(Error("Failed to create SDP answer: description is null")))
                            return
                        }

                        setLocalDescription(description, object : SetSessionDescriptionObserver {
                            override fun onSuccess() {
                                Logger.d(TAG, "Set local description successfully")
                                trySend(Result.success(description))
                            }

                            override fun onFailure(error: String?) {
                                Logger.e(TAG, "Failed to set local description: $error")
                            }
                        })
                    }

                    override fun onFailure(error: String?) {
                        trySend(Result.failure(Error("Failed to create SDP answer: $error")))
                    }
                })
            }

            override fun onFailure(error: String?) {
                trySend(Result.failure(Error("Failed to set remote description: $error")))
            }
        })

        awaitClose {  }
    }

    fun addRemoteCandidate(deviceId: DeviceId, remoteCandidate: IceCandidateData) = flow {
        Logger.d(TAG, "Exchanging ICE Candidate for device ${deviceId.value} - ${remoteCandidate.candidate}")
        val session = session[deviceId] ?: run {
            Logger.e(TAG, "No session found for device ${deviceId.value}")
            throw IllegalArgumentException("No session found for device ${deviceId.value}")
        }
        session.localConn!!.addIceCandidate(remoteCandidate.asLibrary())

        emit(remoteCandidate)
    }

    suspend fun closeSession(deviceId: DeviceId) = mutex.withLock {
        session.remove(deviceId)
    }

    companion object {
        private const val TAG = "WebRTCSignalingManager"
    }
}
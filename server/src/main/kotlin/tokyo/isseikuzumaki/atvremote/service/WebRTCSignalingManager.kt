package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tokyo.isseikuzumaki.atvremote.shared.IceCandidateData
import tokyo.isseikuzumaki.atvremote.shared.SdpAnswer
import tokyo.isseikuzumaki.atvremote.shared.SdpOffer
import java.util.concurrent.ConcurrentHashMap

class WebRTCSignalingManager {
    private val mutex = Mutex()
    private val sessionData = ConcurrentHashMap<String, SessionData>()

    data class SessionData(
        var localSdp: String? = null,
        var remoteSdp: String? = null,
        val iceCandidates: MutableList<IceCandidateData> = mutableListOf()
    )

    suspend fun handleOffer(sessionId: String, offer: SdpOffer): SdpAnswer = mutex.withLock {
        val session = sessionData.getOrPut(sessionId) { SessionData() }
        session.remoteSdp = offer.sdp

        // In a real implementation, this would use native WebRTC libraries to generate an Answer
        // For now, we'll return a mock Answer
        val mockAnswer = generateMockAnswer(offer.sdp)
        session.localSdp = mockAnswer

        SdpAnswer(sessionId, sdp = mockAnswer)
    }

    suspend fun addIceCandidate(candidate: IceCandidateData) = mutex.withLock {
        val session = sessionData.getOrPut(candidate.sessionId) { SessionData() }
        session.iceCandidates.add(candidate)
    }

    suspend fun getIceCandidates(sessionId: String): List<IceCandidateData> = mutex.withLock {
        sessionData[sessionId]?.iceCandidates?.toList() ?: emptyList()
    }

    suspend fun removeSession(sessionId: String) = mutex.withLock {
        sessionData.remove(sessionId)
    }

    private fun generateMockAnswer(offerSdp: String): String {
        // In a real implementation, this would use WebRTC libraries to generate a proper Answer
        // This is a simplified mock for demonstration
        return """v=0
o=- 123456789 2 IN IP4 127.0.0.1
s=-
t=0 0
a=group:BUNDLE 0
a=msid-semantic: WMS
m=video 9 UDP/TLS/RTP/SAVPF 96
c=IN IP4 0.0.0.0
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:mock
a=ice-pwd:mockpassword
a=ice-options:trickle
a=fingerprint:sha-256 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
a=setup:active
a=mid:0
a=sendonly
a=rtcp-mux
a=rtcp-rsize
a=rtpmap:96 H264/90000
a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42001f"""
    }
}

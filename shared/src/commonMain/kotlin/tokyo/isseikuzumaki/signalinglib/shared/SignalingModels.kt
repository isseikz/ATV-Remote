package tokyo.isseikuzumaki.signalinglib.shared

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class SessionID(val value: String)

@Serializable
@JvmInline
value class SessionName(val value: String)

@Serializable
data class Session(
    val id: SessionID,
    val name: SessionName,
    val capabilities: List<String>
)

@Serializable
data class SessionRequest(
    val name: SessionName
)

@Serializable
data class SignalingOffer(
    val sessionID: SessionID,
    val sdp: Sdp
)

@Serializable
sealed class SignalingAnswer {
    @Serializable
    data class Answer(
        val sessionID: SessionID,
        val sdp: Sdp,
    ) : SignalingAnswer()

    @Serializable
    data class Candidate(
        val sessionID: SessionID,
        val candidate: IceCandidateData
    ) : SignalingAnswer()
}

@Serializable
data class SignalingCandidate(
    val sessionID: SessionID,
    val candidates: List<IceCandidateData>
)

@Serializable
data class Sdp(val value: String)

@Serializable
data class SdpOffer(
    val sdp: Sdp
)

@Serializable
data class SdpAnswer(
    val sdp: Sdp,
    val candidates: List<IceCandidateData>
)

@Serializable
data class IceCandidateData(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int
)
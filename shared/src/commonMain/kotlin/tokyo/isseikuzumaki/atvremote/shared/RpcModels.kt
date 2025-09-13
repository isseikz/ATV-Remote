package tokyo.isseikuzumaki.atvremote.shared

import kotlinx.serialization.Serializable

@Serializable
data class SdpOffer(
    val type: String,
    val sdp: String
)

@Serializable
data class SdpAnswer(
    val sessionId: String,
    val sdp: String
)

@Serializable
data class IceCandidateData(
    val sessionId: String,
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int
)

@Serializable
data class IceCandidateResponse(
    val candidates: List<IceCandidateData>
)

@Serializable
data class AdbCommand(
    val command: String
)

@Serializable
data class AdbCommandResult(
    val output: String,
    val isSuccess: Boolean
)

package tokyo.isseikuzumaki.atvremote.shared

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline


@Serializable
data class SdpOffer(
    val deviceId: DeviceId,
    val sdp: String
)

@Serializable
data class SdpAnswer(
    val deviceId: DeviceId,
    val sdp: String
)

@Serializable
data class IceCandidateData(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int
)

@Serializable
data class IceCandidateResponse(
    val candidates: List<IceCandidateData>
)

@Serializable
@JvmInline
value class DeviceId(val value: String)

@Serializable
data class AdbDevice(
    val id: DeviceId,
    val name: String,
    val connected: Boolean
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

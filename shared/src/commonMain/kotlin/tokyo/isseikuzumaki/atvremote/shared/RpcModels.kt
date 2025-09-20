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

@Serializable
data class ScreenshotResult(
    val imageData: ByteArray,
    val isSuccess: Boolean,
    val fileName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScreenshotResult

        if (!imageData.contentEquals(other.imageData)) return false
        if (isSuccess != other.isSuccess) return false
        if (fileName != other.fileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageData.contentHashCode()
        result = 31 * result + isSuccess.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}

package tokyo.isseikuzumaki.atvremote.helper.model

import kotlinx.serialization.Serializable

/**
 * Data model for device registration request
 */
@Serializable
data class DeviceRegistration(
    val deviceId: String,
    val fcmToken: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String
)

/**
 * Data model for build notification payload from FCM
 */
@Serializable
data class BuildNotification(
    val downloadUrl: String,
    val branchName: String,
    val commitHash: String,
    val versionName: String,
    val versionCode: Int,
    val buildTimestamp: Long,
    val buildId: String
)

/**
 * Data model for build history entry
 */
@Serializable
data class BuildHistoryEntry(
    val id: String,
    val branchName: String,
    val commitHash: String,
    val versionName: String,
    val versionCode: Int,
    val receivedAt: Long,
    val installedAt: Long? = null,
    val installStatus: InstallStatus = InstallStatus.PENDING
)

/**
 * Installation status for a build
 */
@Serializable
enum class InstallStatus {
    PENDING,
    DOWNLOADING,
    DOWNLOAD_COMPLETE,
    INSTALLING,
    INSTALLED,
    FAILED
}

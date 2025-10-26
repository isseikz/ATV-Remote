package tokyo.isseikuzumaki.signalinglib.demo.server.installer.model

import kotlinx.serialization.Serializable

/**
 * Device registration request from client
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
 * Registered device information
 */
@Serializable
data class RegisteredDevice(
    val deviceId: String,
    val fcmToken: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val registeredAt: Long,
    val lastSeen: Long
)

/**
 * Build notification request from CI/CD
 */
@Serializable
data class BuildNotificationRequest(
    val s3Bucket: String,
    val s3Key: String,
    val branchName: String,
    val commitHash: String,
    val versionName: String,
    val versionCode: Int,
    val buildTimestamp: Long,
    val targetDeviceGroup: String? = null
)

/**
 * FCM notification payload
 */
@Serializable
data class FcmNotificationPayload(
    val downloadUrl: String,
    val branchName: String,
    val commitHash: String,
    val versionName: String,
    val versionCode: Int,
    val buildTimestamp: Long,
    val buildId: String
)

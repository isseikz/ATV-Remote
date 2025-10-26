package tokyo.isseikuzumaki.signalinglib.demo.server.installer.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.slf4j.LoggerFactory
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.FcmNotificationPayload
import tokyo.isseikuzumaki.signalinglib.demo.server.installer.model.RegisteredDevice
import java.io.FileInputStream

/**
 * Service for sending FCM notifications to devices
 */
class FcmNotificationService(
    serviceAccountPath: String
) {
    private val logger = LoggerFactory.getLogger(FcmNotificationService::class.java)

    init {
        try {
            val serviceAccount = FileInputStream(serviceAccountPath)
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
            
            logger.info("Firebase initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase", e)
        }
    }

    /**
     * Send build notification to a single device
     */
    fun sendBuildNotification(
        device: RegisteredDevice,
        payload: FcmNotificationPayload
    ): Boolean {
        return try {
            val message = Message.builder()
                .setToken(device.fcmToken)
                .putData("downloadUrl", payload.downloadUrl)
                .putData("branchName", payload.branchName)
                .putData("commitHash", payload.commitHash)
                .putData("versionName", payload.versionName)
                .putData("versionCode", payload.versionCode.toString())
                .putData("buildTimestamp", payload.buildTimestamp.toString())
                .putData("buildId", payload.buildId)
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            logger.info("Successfully sent notification to device ${device.deviceId}: $response")
            true
        } catch (e: Exception) {
            logger.error("Failed to send notification to device ${device.deviceId}", e)
            false
        }
    }

    /**
     * Send build notification to multiple devices
     */
    fun sendBuildNotificationToDevices(
        devices: List<RegisteredDevice>,
        payload: FcmNotificationPayload
    ): Map<String, Boolean> {
        return devices.associate { device ->
            device.deviceId to sendBuildNotification(device, payload)
        }
    }
}

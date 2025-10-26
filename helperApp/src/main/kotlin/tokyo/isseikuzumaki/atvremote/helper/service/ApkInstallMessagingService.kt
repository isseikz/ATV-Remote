package tokyo.isseikuzumaki.atvremote.helper.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.atvremote.helper.R
import tokyo.isseikuzumaki.atvremote.helper.model.BuildNotification
import tokyo.isseikuzumaki.atvremote.helper.repository.DeviceRepository

/**
 * FCM service that receives build notifications and triggers APK download
 */
class ApkInstallMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "ApkInstallMessaging"
        private const val CHANNEL_ID = "apk_installation"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        message.data.let { data ->
            if (data.isEmpty()) {
                Log.w(TAG, "Empty data payload")
                return
            }

            try {
                val buildNotification = parseBuildNotification(data)
                Log.d(TAG, "Build notification: $buildNotification")
                
                // Start download service
                startApkDownload(buildNotification)
                
                // Save to build history
                saveToHistory(buildNotification)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing build notification", e)
                showErrorNotification("Failed to process build notification")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Register the new token with backend
        serviceScope.launch {
            try {
                val repository = DeviceRepository(applicationContext)
                repository.registerDevice(token)
                Log.d(TAG, "Device registered with new token")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register new token", e)
            }
        }
    }

    private fun parseBuildNotification(data: Map<String, String>): BuildNotification {
        return BuildNotification(
            downloadUrl = data["downloadUrl"] ?: throw IllegalArgumentException("Missing downloadUrl"),
            branchName = data["branchName"] ?: "unknown",
            commitHash = data["commitHash"] ?: "unknown",
            versionName = data["versionName"] ?: "unknown",
            versionCode = data["versionCode"]?.toIntOrNull() ?: 0,
            buildTimestamp = data["buildTimestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
            buildId = data["buildId"] ?: java.util.UUID.randomUUID().toString()
        )
    }

    private fun startApkDownload(buildNotification: BuildNotification) {
        val intent = Intent(this, ApkDownloadService::class.java).apply {
            putExtra("downloadUrl", buildNotification.downloadUrl)
            putExtra("buildId", buildNotification.buildId)
            putExtra("versionName", buildNotification.versionName)
            putExtra("branchName", buildNotification.branchName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun saveToHistory(buildNotification: BuildNotification) {
        serviceScope.launch {
            try {
                val repository = DeviceRepository(applicationContext)
                repository.saveBuildToHistory(buildNotification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save build to history", e)
            }
        }
    }

    private fun showErrorNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("APK Installation Error")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

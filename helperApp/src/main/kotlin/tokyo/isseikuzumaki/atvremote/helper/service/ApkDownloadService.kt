package tokyo.isseikuzumaki.atvremote.helper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import tokyo.isseikuzumaki.atvremote.helper.R
import tokyo.isseikuzumaki.atvremote.helper.utils.ApkDownloader
import java.io.File

/**
 * Foreground service that downloads APK files from signed URLs
 */
class ApkDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    companion object {
        private const val TAG = "ApkDownloadService"
        private const val CHANNEL_ID = "apk_download"
        private const val NOTIFICATION_ID = 100
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra("downloadUrl")
        val buildId = intent?.getStringExtra("buildId")
        val versionName = intent?.getStringExtra("versionName") ?: "unknown"
        val branchName = intent?.getStringExtra("branchName") ?: "unknown"

        if (downloadUrl == null || buildId == null) {
            Log.e(TAG, "Missing required parameters")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start as foreground service
        val notification = createProgressNotification(0, versionName, branchName)
        startForeground(NOTIFICATION_ID, notification)

        // Start download
        currentJob = serviceScope.launch {
            try {
                downloadApk(downloadUrl, buildId, versionName, branchName)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                showFailureNotification()
            } finally {
                stopSelf()
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        currentJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun downloadApk(
        downloadUrl: String,
        buildId: String,
        versionName: String,
        branchName: String
    ) = withContext(Dispatchers.IO) {
        val cacheDir = File(cacheDir, "apks")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val apkFile = File(cacheDir, "${buildId}.apk")
        val downloader = ApkDownloader()

        try {
            downloader.download(
                url = downloadUrl,
                outputFile = apkFile,
                onProgress = { progress ->
                    updateProgressNotification(progress, versionName, branchName)
                }
            )

            Log.d(TAG, "Download complete: ${apkFile.absolutePath}")
            showInstallNotification(apkFile, versionName, branchName)

        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            apkFile.delete()
            throw e
        }
    }

    private fun createProgressNotification(
        progress: Int,
        versionName: String,
        branchName: String
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading APK")
            .setContentText("$versionName ($branchName)")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateProgressNotification(
        progress: Int,
        versionName: String,
        branchName: String
    ) {
        val notification = createProgressNotification(progress, versionName, branchName)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showInstallNotification(apkFile: File, versionName: String, branchName: String) {
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("APK Ready to Install")
            .setContentText("$versionName ($branchName) - Tap to install")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download Failed")
            .setContentText("Failed to download APK")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "APK download progress notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

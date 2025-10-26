package tokyo.isseikuzumaki.atvremote.helper.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.atvremote.helper.BuildConfig
import tokyo.isseikuzumaki.atvremote.helper.model.BuildHistoryEntry
import tokyo.isseikuzumaki.atvremote.helper.model.BuildNotification
import tokyo.isseikuzumaki.atvremote.helper.model.DeviceRegistration
import tokyo.isseikuzumaki.atvremote.helper.model.InstallStatus

/**
 * Repository for managing device registration and build history
 */
class DeviceRepository(private val context: Context) {

    companion object {
        private const val TAG = "DeviceRepository"
        private const val PREFS_NAME = "device_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_BUILD_HISTORY = "build_history"
        
        // Default server URL - should be configured by user
        private const val DEFAULT_SERVER_URL = "https://your-server.com"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Register device with backend server
     */
    suspend fun registerDevice(fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getOrCreateDeviceId()
            val serverUrl = getServerUrl()
            
            val registration = DeviceRegistration(
                deviceId = deviceId,
                fcmToken = fcmToken,
                deviceModel = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                appVersion = BuildConfig.VERSION_NAME
            )

            val response = client.post("$serverUrl/api/devices/register") {
                contentType(ContentType.Application.Json)
                setBody(registration)
            }

            val success = response.status.isSuccess()
            Log.d(TAG, "Device registration ${if (success) "successful" else "failed"}: ${response.status}")
            success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
            false
        }
    }

    /**
     * Save build notification to history
     */
    suspend fun saveBuildToHistory(buildNotification: BuildNotification) = withContext(Dispatchers.IO) {
        try {
            val historyEntry = BuildHistoryEntry(
                id = buildNotification.buildId,
                branchName = buildNotification.branchName,
                commitHash = buildNotification.commitHash,
                versionName = buildNotification.versionName,
                versionCode = buildNotification.versionCode,
                receivedAt = System.currentTimeMillis(),
                installStatus = InstallStatus.PENDING
            )

            val history = getBuildHistory().toMutableList()
            history.add(0, historyEntry) // Add to beginning
            
            // Keep only last 50 builds
            if (history.size > 50) {
                history.subList(50, history.size).clear()
            }

            saveBuildHistory(history)
            Log.d(TAG, "Build saved to history: ${buildNotification.buildId}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save build to history", e)
        }
    }

    /**
     * Update build status in history
     */
    suspend fun updateBuildStatus(buildId: String, status: InstallStatus, installedAt: Long? = null) = withContext(Dispatchers.IO) {
        try {
            val history = getBuildHistory().toMutableList()
            val index = history.indexOfFirst { it.id == buildId }
            
            if (index >= 0) {
                history[index] = history[index].copy(
                    installStatus = status,
                    installedAt = installedAt
                )
                saveBuildHistory(history)
                Log.d(TAG, "Build status updated: $buildId -> $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update build status", e)
        }
    }

    /**
     * Get build history
     */
    fun getBuildHistory(): List<BuildHistoryEntry> {
        return try {
            val historyJson = prefs.getString(KEY_BUILD_HISTORY, null) ?: return emptyList()
            json.decodeFromString(historyJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load build history", e)
            emptyList()
        }
    }

    /**
     * Get or create device ID
     */
    fun getOrCreateDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                java.util.UUID.randomUUID().toString()
            }
            
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        return deviceId
    }

    /**
     * Get server URL
     */
    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    /**
     * Set server URL
     */
    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    private fun saveBuildHistory(history: List<BuildHistoryEntry>) {
        val historyJson = json.encodeToString(history)
        prefs.edit().putString(KEY_BUILD_HISTORY, historyJson).apply()
    }

    fun close() {
        client.close()
    }
}

package tokyo.isseikuzumaki.atvremote.helper

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.atvremote.helper.repository.DeviceRepository

/**
 * Application class for the helper app
 */
class HelperApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "HelperApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Get FCM token and register device
        applicationScope.launch {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d(TAG, "FCM Token: $token")
                        
                        // Register device with backend
                        applicationScope.launch {
                            val repository = DeviceRepository(this@HelperApplication)
                            val success = repository.registerDevice(token)
                            Log.d(TAG, "Device registration: ${if (success) "success" else "failed"}")
                        }
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.exception)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FCM", e)
            }
        }
    }
}

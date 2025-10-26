package tokyo.isseikuzumaki.atvremote.helper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.atvremote.helper.model.BuildHistoryEntry
import tokyo.isseikuzumaki.atvremote.helper.repository.DeviceRepository
import tokyo.isseikuzumaki.atvremote.helper.ui.theme.HelperAppTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main activity for the helper app
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: DeviceRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val TAG = "MainActivity"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = DeviceRepository(this)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            HelperAppTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var showSettings by remember { mutableStateOf(false) }
        var buildHistory by remember { mutableStateOf(repository.getBuildHistory()) }
        var fcmToken by remember { mutableStateOf("Loading...") }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fcmToken = task.result
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ATV Remote Helper") },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            if (showSettings) {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    fcmToken = fcmToken,
                    deviceId = repository.getOrCreateDeviceId(),
                    serverUrl = repository.getServerUrl(),
                    onServerUrlChange = { url ->
                        repository.setServerUrl(url)
                    },
                    onCheckInstallPermission = {
                        checkInstallPermission()
                    }
                )
            } else {
                BuildHistoryScreen(
                    modifier = Modifier.padding(padding),
                    builds = buildHistory,
                    onRefresh = {
                        buildHistory = repository.getBuildHistory()
                    }
                )
            }
        }
    }

    @Composable
    fun BuildHistoryScreen(
        modifier: Modifier = Modifier,
        builds: List<BuildHistoryEntry>,
        onRefresh: () -> Unit
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Build History",
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            if (builds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No builds received yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(builds) { build ->
                        BuildHistoryItem(build)
                    }
                }
            }
        }
    }

    @Composable
    fun BuildHistoryItem(build: BuildHistoryEntry) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = build.versionName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Branch: ${build.branchName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Commit: ${build.commitHash.take(8)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Received: ${dateFormat.format(Date(build.receivedAt))}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Status: ${build.installStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (build.installStatus.name) {
                        "INSTALLED" -> MaterialTheme.colorScheme.primary
                        "FAILED" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }

    @Composable
    fun SettingsScreen(
        modifier: Modifier = Modifier,
        fcmToken: String,
        deviceId: String,
        serverUrl: String,
        onServerUrlChange: (String) -> Unit,
        onCheckInstallPermission: () -> Unit
    ) {
        var editableServerUrl by remember { mutableStateOf(serverUrl) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = editableServerUrl,
                onValueChange = { editableServerUrl = it },
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onServerUrlChange(editableServerUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Server URL")
            }

            Divider()

            Text("Device ID: $deviceId", style = MaterialTheme.typography.bodyMedium)
            Text("FCM Token: ${fcmToken.take(20)}...", style = MaterialTheme.typography.bodySmall)

            Divider()

            Button(
                onClick = onCheckInstallPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Install Permission")
            }
        }
    }

    private fun checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                // Redirect to settings
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Log.d(TAG, "Install permission already granted")
            }
        }
    }
}

package tokyo.isseikuzumaki.signalinglib.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import signalinglib.demo.composeapp.generated.resources.Res
import signalinglib.demo.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.flow.launchIn
import org.jetbrains.compose.resources.painterResource
import tokyo.isseikuzumaki.signalinglib.demo.components.Video
import tokyo.isseikuzumaki.signalinglib.demo.components.ExpandableDropdownMenu
import tokyo.isseikuzumaki.signalinglib.demo.components.DropdownData
import tokyo.isseikuzumaki.signalinglib.demo.components.DPadComponent
import tokyo.isseikuzumaki.signalinglib.demo.components.ScreenshotButton
import tokyo.isseikuzumaki.signalinglib.demo.viewmodel.AppViewModel

@Composable
fun App(
    viewModel: AppViewModel = AppViewModel()
) {
    val selectedVideo by viewModel.activeVideo.collectAsStateWithLifecycle(initialValue = null)
    val adbDevices by viewModel.adbDevices.collectAsStateWithLifecycle(initialValue = emptyList())
    val waitingList by viewModel.waitingList.collectAsStateWithLifecycle(initialValue = emptyList())

    val scope = rememberCoroutineScope()
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                selectedVideo?.let {
                    Video(
                        videoTrack = it,
                        audioTrack = null,
                        modifier = Modifier.fillMaxWidth().background(color = Color.Black)
                    )
                } ?: run {
                    Image(
                        painter = painterResource(Res.drawable.compose_multiplatform),
                        contentDescription = "Compose Multiplatform",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("No video")
                }
            }

            Row {
                // Select Video Source
                ExpandableDropdownMenu(
                    DropdownData(
                        label = "Video",
                        items = waitingList.map { session ->
                            DropdownData.Item(
                                title = "${session.id}",
                                onClick = {
                                    viewModel.select(session.id)
                                }
                            )
                        }
                    )
                )

                // Select ADB Device
                ExpandableDropdownMenu(
                    DropdownData(
                        label = "ADB",
                        items = adbDevices.map { device ->
                            DropdownData.Item(
                                title = device.name,
                                onClick = {
                                    viewModel.select(device.id)
                                }
                            )
                        },
                    )
                )
            }

            // DPAD Component
            DPadComponent(
                onDPadCenterClick = {
                    viewModel.sendAdbCommand("input keyevent 23") // KEYCODE_DPAD_CENTER
                        .launchIn(scope)
                },
                onDPadUpClick = {
                    viewModel.sendAdbCommand("input keyevent 19") // KEYCODE_DPAD_UP
                        .launchIn(scope)
                },
                onDPadDownClick = {
                    viewModel.sendAdbCommand("input keyevent 20") // KEYCODE_DPAD_DOWN
                        .launchIn(scope)
                },
                onDPadLeftClick = {
                    viewModel.sendAdbCommand("input keyevent 21") // KEYCODE_DPAD_LEFT
                        .launchIn(scope)
                },
                onDPadRightClick = {
                    viewModel.sendAdbCommand("input keyevent 22") // KEYCODE_DPAD_RIGHT
                        .launchIn(scope)
                },
                onBackClick = {
                    viewModel.sendAdbCommand("input keyevent 4") // KEYCODE_BACK
                        .launchIn(scope)
                },
                onHomeClick = {
                    viewModel.sendAdbCommand("input keyevent 3") // KEYCODE_HOME
                        .launchIn(scope)
                }
            )

            ScreenshotButton(viewModel)
        }
    }
}
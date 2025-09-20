package tokyo.isseikuzumaki.atvremote

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import atv_remote.composeapp.generated.resources.Res
import atv_remote.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.flow.launchIn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.atvremote.viewmodel.AppViewModel

@Composable
@Preview
fun App(
    viewModel: AppViewModel
) {
    val activeVideo by viewModel.activeVideo.collectAsState(initial = null)
    val devices by viewModel.observeDevices().collectAsState(initial = emptyList())
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
                activeVideo?.let {
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
                ExpandableDropdownMenu(
                    DropdownData(
                        items = devices.map { device ->
                            DropdownData.Item(
                                title = "${device.name} - ${device.id.value}",
                                onClick = {
                                    viewModel.selectDevice(device.id)
                                }
                            )
                        }
                    )
                )

                ConnectButton(onClick = {
                    viewModel.openVideo()
                })
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
        }
    }
}

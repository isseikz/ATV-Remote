package tokyo.isseikuzumaki.atvremote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import atv_remote.composeapp.generated.resources.Res
import atv_remote.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.atvremote.viewmodel.AppViewModel

@Composable
@Preview
fun App(
    viewModel: AppViewModel
) {
    val activeVideo by viewModel.activeVideo.collectAsState(initial = null)
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (activeVideo == null) {
                Image(
                    painter = painterResource(Res.drawable.compose_multiplatform),
                    contentDescription = "Compose Multiplatform",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("No video")

                ConnectButton(onClick = {
                    viewModel.openVideo()
                })
            } else {
                Video(videoTrack = activeVideo!!, modifier = Modifier.weight(1f))
            }
        }
    }
}

package tokyo.isseikuzumaki.signalinglib.demo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import tokyo.isseikuzumaki.signalinglib.demo.viewmodel.AppViewModel

@Composable
fun ScreenshotButton(
    viewModel: AppViewModel
) {
    val scope = rememberCoroutineScope()
    var screenshotBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var screenshotStatus by remember { mutableStateOf<String?>(null) }
    IconButton(
        onClick = {
            screenshotStatus = "Taking screenshot..."
            viewModel.takeScreenshot()
                .onEach { result ->
                    result.onSuccess { screenshotResult ->
                        try {
                            // Convert ByteArray to ImageBitmap
                            screenshotBitmap = screenshotResult.imageData.decodeToImageBitmap()
                            screenshotStatus = "Screenshot taken successfully!"

                        } catch (e: Exception) {
                            screenshotStatus = "Failed to display screenshot: ${e.message}"
                        }
                    }.onFailure { exception ->
                        screenshotStatus = "Screenshot failed: ${exception.message}"
                        screenshotBitmap = null
                    }
                }
                .launchIn(scope)
        },
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = "Take Screenshot",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }

    // Screenshot Display Section
    screenshotStatus?.let { status ->
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(8.dp)
        )
    }

    screenshotBitmap?.let { bitmap ->
        Card(
            modifier = Modifier
                .padding(16.dp)
                .size(200.dp, 300.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Device Screenshot",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
package tokyo.isseikuzumaki.signalinglib.demo.components

import WebRTC.RTCMTLVideoView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.shepeliev.webrtckmp.AudioTrack
import com.shepeliev.webrtckmp.VideoTrack
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIViewContentMode
import tokyo.isseikuzumaki.signalinglib.shared.Logger

private const val TAG = "Video.ios"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Video(
    videoTrack: VideoTrack,
    modifier: Modifier,
    audioTrack: AudioTrack?,
) {
    var aspectRatio by remember { mutableStateOf(16f / 9f) }

    LaunchedEffect(videoTrack) {
        videoTrack.settings.also {
            Logger.d(TAG, "Video settings: ${it.width} x ${it.height} @ ${it.frameRate} fps")

        }
    }

    UIKitView(
        factory = {
            RTCMTLVideoView().apply {
                videoContentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                videoTrack.addRenderer(this)
            }
        },
        modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio),
        update = { /* NoOp */ },
        onRelease = { videoTrack.removeRenderer(it) },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )
}
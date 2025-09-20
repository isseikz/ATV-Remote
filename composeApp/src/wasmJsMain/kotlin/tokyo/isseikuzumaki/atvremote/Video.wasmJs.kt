package tokyo.isseikuzumaki.atvremote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import com.shepeliev.webrtckmp.AudioTrack
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.VideoTrack
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.MediaProvider
import kotlin.math.max

@Composable
actual fun Video(
    videoTrack: VideoTrack,
    modifier: Modifier,
    audioTrack: AudioTrack?,
) {
    val stream = remember { MediaStream() }
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }
    val aspectRatioModifier =
        if (videoWidth > 0 && videoHeight > 0) {
            Modifier.aspectRatio(videoWidth.toFloat() / videoHeight.toFloat())
        } else Modifier

    val videoElement =
        remember {
            (document.createElement("video") as HTMLVideoElement).apply {
                srcObject = stream.js as MediaProvider
                autoplay = true
                style.position = "absolute"
                style.objectFit = "contain"
                muted = true
            }
        }

    DisposableEffect(videoElement, stream) {
        document.body?.appendChild(videoElement)
        onDispose {
            document.body?.removeChild(videoElement)
            videoElement.srcObject = null
            stream.removeTrack(videoTrack)
            audioTrack?.let { stream.removeTrack(it) }
            stream.release()
        }
    }

    DisposableEffect(videoTrack) {
        stream.addTrack(videoTrack)
        onDispose { stream.removeTrack(videoTrack) }
    }

    DisposableEffect(audioTrack) {
        audioTrack?.let { stream.addTrack(it) }
        onDispose { audioTrack?.let { stream.removeTrack(it) } }
    }

    // Update intrinsic size
    LaunchedEffect(videoElement) {
        fun update() {
            val w = max(1, videoElement.videoWidth)
            val h = max(1, videoElement.videoHeight)
            if (w != videoWidth || h != videoHeight) {
                videoWidth = w
                videoHeight = h
            }
        }
        videoElement.onloadedmetadata = { update(); null }
        // Some browsers fire resize when track changes resolution
        videoElement.addEventListener("resize", { update() })
    }

    val density = LocalDensity.current

    Box(
        modifier =
            modifier
                then(aspectRatioModifier)
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        with(videoElement.style) {
                            top = "${coordinates.positionInWindow().y.toDp().value}px"
                            left = "${coordinates.positionInWindow().x.toDp().value}px"
                            width = "${coordinates.size.width.toDp().value}px"
                            height = "${coordinates.size.height.toDp().value}px"
                        }
                    }
                },
    )
}

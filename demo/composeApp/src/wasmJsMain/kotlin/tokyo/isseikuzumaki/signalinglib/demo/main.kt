package tokyo.isseikuzumaki.signalinglib.demo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import tokyo.isseikuzumaki.signalinglib.demo.viewmodel.AppViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App(AppViewModel())
    }
}
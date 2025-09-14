package tokyo.isseikuzumaki.atvremote

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import tokyo.isseikuzumaki.atvremote.viewmodel.AppViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App(AppViewModel())
    }
}

package tokyo.isseikuzumaki.atvremote

import androidx.compose.ui.window.ComposeUIViewController
import tokyo.isseikuzumaki.atvremote.viewmodel.AppViewModel

fun MainViewController() = ComposeUIViewController {
    App(AppViewModel())
}
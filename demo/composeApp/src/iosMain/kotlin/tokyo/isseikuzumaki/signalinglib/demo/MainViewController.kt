package tokyo.isseikuzumaki.signalinglib.demo

import androidx.compose.ui.window.ComposeUIViewController
import tokyo.isseikuzumaki.signalinglib.demo.viewmodel.AppViewModel

fun MainViewController() = ComposeUIViewController {
    App(AppViewModel())
}
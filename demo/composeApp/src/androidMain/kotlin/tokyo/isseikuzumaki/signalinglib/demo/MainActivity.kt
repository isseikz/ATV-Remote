package tokyo.isseikuzumaki.signalinglib.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.signalinglib.demo.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(AppViewModel())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(AppViewModel())
}
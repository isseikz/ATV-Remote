package tokyo.isseikuzumaki.atvremote

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ConnectButton(onClick: () -> Unit, modifier: Modifier = Modifier)

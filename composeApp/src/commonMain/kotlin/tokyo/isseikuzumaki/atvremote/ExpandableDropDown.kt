package tokyo.isseikuzumaki.atvremote

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

data class DropdownData(
    val items: List<Item>
) {
    class Item(
        val title: String,
        val onClick: () -> Unit
    )
}

@Composable
fun ExpandableDropdownMenu(
    data: DropdownData
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
    ) {
        if (!expanded) {
            Button(
                onClick = { expanded = true }
            ) {
                Text("Open Menu")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            data.items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.title) },
                    onClick = {
                        item.onClick()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun ExpandableDropdownMenuPreview() {
    MaterialTheme {
        Surface {
            ExpandableDropdownMenu(
                data = DropdownData(
                    items = listOf(
                        DropdownData.Item(
                            title = "Item 1",
                            onClick = {}
                        ),
                        DropdownData.Item(
                            title = "Item 2",
                            onClick = {}
                        ),
                        DropdownData.Item(
                            title = "Item 3",
                            onClick = {}
                        ),
                    )
                )
            )
        }
    }
}
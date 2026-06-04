package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SelectionButton(
    label: String,
    value: String,
    items: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Button(
            enabled = items.isNotEmpty(),
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "$label: $value",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 260.dp),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(item)
                    },
                )
            }
        }
    }
}

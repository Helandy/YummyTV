package su.afk.yummy.tv.feature.messages.view

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.messages.mobile.R

@Composable
internal fun ChatMobileActionsMenu(
    isBanned: Boolean,
    enabled: Boolean,
    onBanToggle: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.messages_dialog_actions)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (isBanned) R.string.messages_unban else R.string.messages_ban)) },
                leadingIcon = {
                    Icon(
                        if (isBanned) Icons.Filled.PersonAdd else Icons.Filled.Block,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onBanToggle()
                },
            )
        }
    }
}

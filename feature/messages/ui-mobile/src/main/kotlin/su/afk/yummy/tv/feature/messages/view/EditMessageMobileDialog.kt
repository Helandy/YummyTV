package su.afk.yummy.tv.feature.messages.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.messages.mobile.R

@Composable
internal fun EditMessageMobileDialog(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.messages_edit_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                minLines = 3,
                maxLines = 8,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = enabled && text.isNotBlank()) {
                Text(stringResource(R.string.messages_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = enabled) {
                Text(stringResource(R.string.messages_cancel))
            }
        },
    )
}

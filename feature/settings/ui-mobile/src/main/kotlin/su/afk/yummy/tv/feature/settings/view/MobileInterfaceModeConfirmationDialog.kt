package su.afk.yummy.tv.feature.settings.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.settings.mobile.R

@Composable
internal fun MobileInterfaceModeConfirmationDialog(
    targetModeLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_interface_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.settings_interface_confirm_message,
                    targetModeLabel,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_interface_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_interface_cancel))
            }
        },
    )
}

package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.library.mobile.R

@Composable
internal fun LibraryMobileRemoveConfirmDialog(
    title: String,
    listTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_mobile_remove_title)) },
        text = {
            Text(
                stringResource(
                    R.string.library_mobile_remove_message,
                    title,
                    listTitle,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.library_mobile_remove_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_mobile_remove_cancel))
            }
        },
    )
}

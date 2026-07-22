package su.afk.yummy.tv.feature.videodownload.mobile.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.videodownload.mobile.R

@Composable
internal fun VideoExportReExportConfirmationDialog(
    animeTitle: String,
    episode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.video_export_reexport_dialog_title)) },
        text = {
            Text(
                stringResource(
                    R.string.video_export_reexport_dialog_message,
                    animeTitle,
                    episode,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.video_export_reexport_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.video_export_reexport_dialog_cancel))
            }
        },
    )
}

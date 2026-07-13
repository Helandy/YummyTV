package su.afk.yummy.tv.feature.videodownload.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.videodownload.mobile.R

@Composable
internal fun VideoDownloadDeleteConfirmationDialog(
    animeTitle: String,
    episode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.video_download_delete_confirm_title, episode))
        },
        text = {
            Text(stringResource(R.string.video_download_delete_confirm_message, animeTitle))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.video_download_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.video_download_delete_cancel))
            }
        },
    )
}

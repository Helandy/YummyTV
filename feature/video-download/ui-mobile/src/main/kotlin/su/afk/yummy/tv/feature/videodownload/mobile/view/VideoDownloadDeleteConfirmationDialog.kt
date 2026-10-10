package su.afk.yummy.tv.feature.videodownload.mobile.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.videodownload.presentation.R

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
                    text = stringResource(CoreR.string.common_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.common_cancel))
            }
        },
    )
}

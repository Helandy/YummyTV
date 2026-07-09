package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun EpisodeDownloadDeleteConfirmationDialog(
    episode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.details_mobile_delete_downloaded_episode_title,
                    episode,
                )
            )
        },
        text = { Text(stringResource(R.string.details_mobile_delete_downloaded_episode_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.details_mobile_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_mobile_delete_cancel))
            }
        },
    )
}

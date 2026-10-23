package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun EpisodeDownloadDeleteConfirmationDialog(
    episode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.details_mobile_delete_downloaded_episode_title,
                    episode,
                )
            )
        },
        confirmText = stringResource(CoreR.string.common_delete),
        onConfirm = onConfirm,
        destructive = true,
        dismissText = stringResource(CoreR.string.common_cancel),
        content = { Text(stringResource(R.string.details_mobile_delete_downloaded_episode_message)) },
    )
}

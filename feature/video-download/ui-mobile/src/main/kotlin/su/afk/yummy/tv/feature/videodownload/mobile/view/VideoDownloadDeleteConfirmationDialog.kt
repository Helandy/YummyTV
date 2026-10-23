package su.afk.yummy.tv.feature.videodownload.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.videodownload.presentation.R

@Composable
internal fun VideoDownloadDeleteConfirmationDialog(
    animeTitle: String,
    episode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.video_download_delete_confirm_title, episode))
        },
        confirmText = stringResource(CoreR.string.common_delete),
        onConfirm = onConfirm,
        destructive = true,
        dismissText = stringResource(CoreR.string.common_cancel),
        content = {
            Text(stringResource(R.string.video_download_delete_confirm_message, animeTitle))
        },
    )
}

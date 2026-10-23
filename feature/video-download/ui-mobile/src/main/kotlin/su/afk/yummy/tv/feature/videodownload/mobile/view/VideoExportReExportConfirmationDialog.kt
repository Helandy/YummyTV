package su.afk.yummy.tv.feature.videodownload.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.videodownload.presentation.R

@Composable
internal fun VideoExportReExportConfirmationDialog(
    animeTitle: String,
    episode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.video_export_reexport_dialog_title)) },
        confirmText = stringResource(R.string.video_export_reexport_dialog_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(CoreR.string.common_cancel),
        onDismiss = onDismiss,
        content = {
            Text(
                stringResource(
                    R.string.video_export_reexport_dialog_message,
                    animeTitle,
                    episode,
                )
            )
        },
    )
}

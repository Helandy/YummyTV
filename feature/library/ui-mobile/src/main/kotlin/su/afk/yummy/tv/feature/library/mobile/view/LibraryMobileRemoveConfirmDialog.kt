package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.library.presentation.R

@Composable
internal fun LibraryMobileRemoveConfirmDialog(
    title: String,
    listTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_mobile_remove_title)) },
        confirmText = stringResource(R.string.library_mobile_remove_content_description),
        onConfirm = onConfirm,
        dismissText = stringResource(CoreR.string.common_cancel),
        content = {
            Text(
                stringResource(
                    R.string.library_mobile_remove_message,
                    title,
                    listTitle,
                )
            )
        },
    )
}

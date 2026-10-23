package su.afk.yummy.tv.feature.collection.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.collection.presentation.R

@Composable
internal fun MobileDeleteCollectionDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.collection_delete_dialog_title)) },
        confirmText = stringResource(CoreR.string.common_delete),
        onConfirm = onConfirm,
        confirmEnabled = !isDeleting,
        confirmLoading = isDeleting,
        dismissText = stringResource(CoreR.string.common_cancel),
        onDismiss = onDismiss,
        dismissEnabled = !isDeleting,
        content = { Text(stringResource(R.string.collection_delete_dialog_message)) },
    )
}

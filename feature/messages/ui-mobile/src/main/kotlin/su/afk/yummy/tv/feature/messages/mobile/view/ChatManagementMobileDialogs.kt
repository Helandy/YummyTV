package su.afk.yummy.tv.feature.messages.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.messages.presentation.R

@Composable
internal fun ChatManagementMobileDialogs(
    showDeleteConfirmation: Boolean,
    showClaimConfirmation: Boolean,
    showBanConfirmation: Boolean,
    isBanned: Boolean,
    isMutating: Boolean,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onClaimConfirm: () -> Unit,
    onClaimDismiss: () -> Unit,
    onBanConfirm: () -> Unit,
    onBanDismiss: () -> Unit,
) {
    if (showDeleteConfirmation) {
        MobileAlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.messages_delete_title)) },
            confirmText = stringResource(CoreR.string.common_delete),
            onConfirm = onDeleteConfirm,
            confirmEnabled = !isMutating,
            dismissText = stringResource(CoreR.string.common_cancel),
            onDismiss = onDeleteDismiss,
            dismissEnabled = !isMutating,
            content = { Text(stringResource(R.string.messages_delete_confirmation)) },
        )
    }

    if (showClaimConfirmation) {
        MobileAlertDialog(
            onDismissRequest = onClaimDismiss,
            title = { Text(stringResource(R.string.messages_claim_title)) },
            confirmText = stringResource(R.string.messages_claim),
            onConfirm = onClaimConfirm,
            confirmEnabled = !isMutating,
            dismissText = stringResource(CoreR.string.common_cancel),
            onDismiss = onClaimDismiss,
            dismissEnabled = !isMutating,
            content = { Text(stringResource(R.string.messages_claim_description)) },
        )
    }

    if (showBanConfirmation) {
        MobileAlertDialog(
            onDismissRequest = onBanDismiss,
            title = {
                Text(stringResource(if (isBanned) R.string.messages_unban_title else R.string.messages_ban_title))
            },
            confirmText = stringResource(if (isBanned) R.string.messages_unban else R.string.messages_ban),
            onConfirm = onBanConfirm,
            confirmEnabled = !isMutating,
            dismissText = stringResource(CoreR.string.common_cancel),
            onDismiss = onBanDismiss,
            dismissEnabled = !isMutating,
            content = {
                Text(
                    stringResource(
                        if (isBanned) R.string.messages_unban_confirmation
                        else R.string.messages_ban_confirmation
                    )
                )
            },
        )
    }
}

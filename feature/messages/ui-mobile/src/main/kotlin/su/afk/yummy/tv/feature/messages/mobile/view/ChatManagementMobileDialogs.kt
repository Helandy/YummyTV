package su.afk.yummy.tv.feature.messages.mobile.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.messages.mobile.R

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
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.messages_delete_title)) },
            text = { Text(stringResource(R.string.messages_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm, enabled = !isMutating) {
                    Text(stringResource(R.string.messages_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss, enabled = !isMutating) {
                    Text(stringResource(R.string.messages_cancel))
                }
            },
        )
    }

    if (showClaimConfirmation) {
        AlertDialog(
            onDismissRequest = onClaimDismiss,
            title = { Text(stringResource(R.string.messages_claim_title)) },
            text = { Text(stringResource(R.string.messages_claim_description)) },
            confirmButton = {
                TextButton(onClick = onClaimConfirm, enabled = !isMutating) {
                    Text(stringResource(R.string.messages_claim))
                }
            },
            dismissButton = {
                TextButton(onClick = onClaimDismiss, enabled = !isMutating) {
                    Text(stringResource(R.string.messages_cancel))
                }
            },
        )
    }

    if (showBanConfirmation) {
        AlertDialog(
            onDismissRequest = onBanDismiss,
            title = {
                Text(stringResource(if (isBanned) R.string.messages_unban_title else R.string.messages_ban_title))
            },
            text = {
                Text(
                    stringResource(
                        if (isBanned) R.string.messages_unban_confirmation
                        else R.string.messages_ban_confirmation
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onBanConfirm, enabled = !isMutating) {
                    Text(stringResource(if (isBanned) R.string.messages_unban else R.string.messages_ban))
                }
            },
            dismissButton = {
                TextButton(onClick = onBanDismiss, enabled = !isMutating) {
                    Text(stringResource(R.string.messages_cancel))
                }
            },
        )
    }
}

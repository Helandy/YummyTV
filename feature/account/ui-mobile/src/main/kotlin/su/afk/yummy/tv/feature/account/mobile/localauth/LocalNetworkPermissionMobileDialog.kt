package su.afk.yummy.tv.feature.account.mobile.localauth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.permissions.LocalNetworkPermissionStep
import su.afk.yummy.tv.feature.account.mobile.R

/** Объяснение перед системным запросом доступа к локальной сети. */
@Composable
internal fun LocalNetworkPermissionMobileDialog(
    step: LocalNetworkPermissionStep,
    onConfirm: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (step == LocalNetworkPermissionStep.Idle) return

    val isBlocked = step == LocalNetworkPermissionStep.Blocked

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_mobile_local_network_permission_title)) },
        text = {
            Text(
                stringResource(
                    if (isBlocked) {
                        R.string.account_mobile_local_network_permission_blocked_message
                    } else {
                        R.string.account_mobile_local_network_permission_message
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = if (isBlocked) onOpenSettings else onConfirm) {
                Text(
                    stringResource(
                        if (isBlocked) {
                            R.string.account_mobile_local_network_permission_open_settings
                        } else {
                            R.string.account_mobile_local_network_permission_allow
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.account_mobile_local_network_permission_later))
            }
        },
    )
}

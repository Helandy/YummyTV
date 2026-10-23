package su.afk.yummy.tv.feature.account.mobile.localauth

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.core.designsystem.permissions.LocalNetworkPermissionStep
import su.afk.yummy.tv.feature.account.presentation.R

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

    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_local_network_permission_title)) },
        confirmText = stringResource(
                        if (isBlocked) {
                            R.string.account_local_network_permission_open_settings
                        } else {
                            R.string.account_local_network_permission_allow
                        },
                    ),
        onConfirm = if (isBlocked) onOpenSettings else onConfirm,
        dismissText = stringResource(R.string.account_local_network_permission_later),
        onDismiss = onDismiss,
        content = {
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
    )
}

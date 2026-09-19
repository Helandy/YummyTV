package su.afk.yummy.tv.feature.account.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.permissions.LocalNetworkPermissionStep
import su.afk.yummy.tv.feature.account.R

/**
 * Объяснение перед системным запросом доступа к локальной сети: пульт не подскажет,
 * зачем приложению сеть, поэтому говорим об этом сами.
 */
@Composable
internal fun LocalNetworkPermissionTvDialog(
    step: LocalNetworkPermissionStep,
    onConfirm: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (step == LocalNetworkPermissionStep.Idle) return

    val isBlocked = step == LocalNetworkPermissionStep.Blocked
    // Без явного запроса фокус уходит из диалога и открывается боковое меню.
    val confirmFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isBlocked) { requestFocusUntilTimeout(confirmFocusRequester) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_local_network_permission_title)) },
        text = {
            Text(
                stringResource(
                    if (isBlocked) {
                        R.string.account_local_network_permission_blocked_message
                    } else {
                        R.string.account_local_network_permission_message
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = if (isBlocked) onOpenSettings else onConfirm,
                modifier = Modifier.focusRequester(confirmFocusRequester),
            ) {
                Text(
                    stringResource(
                        if (isBlocked) {
                            R.string.account_local_network_permission_open_settings
                        } else {
                            R.string.account_local_network_permission_allow
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.account_local_network_permission_later))
            }
        },
    )
}

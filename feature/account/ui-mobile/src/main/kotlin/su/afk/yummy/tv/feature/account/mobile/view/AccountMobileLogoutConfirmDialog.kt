package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun AccountMobileLogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_logout_confirm_title)) },
        confirmText = stringResource(R.string.account_logout),
        onConfirm = onConfirm,
        dismissText = stringResource(CoreR.string.common_cancel),
        content = { Text(stringResource(R.string.account_logout_confirm_message)) },
    )
}

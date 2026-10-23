package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.settings.presentation.R

@Composable
internal fun MobileInterfaceModeConfirmationDialog(
    targetModeLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_interface_confirm_title)) },
        confirmText = stringResource(R.string.settings_interface_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(CoreR.string.common_cancel),
        onDismiss = onDismiss,
        content = {
            Text(
                stringResource(
                    R.string.settings_interface_confirm_message,
                    targetModeLabel,
                )
            )
        },
    )
}

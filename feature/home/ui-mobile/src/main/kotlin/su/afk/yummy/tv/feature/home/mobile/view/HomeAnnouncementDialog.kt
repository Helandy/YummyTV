@file:JvmName("MobileHomeAnnouncementDialogKt")

package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.home.presentation.R

@Composable
internal fun HomeAnnouncementDialog(
    title: String?,
    message: String,
    buttonText: String?,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = title?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        confirmText = buttonText?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.home_announcement_ok),
        onConfirm = onDismiss,
        content = { Text(message) },
    )
}

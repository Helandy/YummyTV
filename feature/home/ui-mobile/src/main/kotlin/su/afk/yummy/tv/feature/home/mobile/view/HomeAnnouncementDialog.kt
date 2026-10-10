@file:JvmName("MobileHomeAnnouncementDialogKt")

package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.home.presentation.R

@Composable
internal fun HomeAnnouncementDialog(
    title: String?,
    message: String,
    buttonText: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    buttonText?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.home_announcement_ok),
                )
            }
        },
    )
}

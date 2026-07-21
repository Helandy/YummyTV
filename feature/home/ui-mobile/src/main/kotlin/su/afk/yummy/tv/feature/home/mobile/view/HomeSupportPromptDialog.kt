@file:JvmName("MobileHomeSupportPromptDialogKt")

package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.home.mobile.R

@Composable
internal fun HomeSupportPromptDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_support_prompt_title)) },
        text = { Text(stringResource(R.string.home_support_prompt_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.home_support_prompt_ok))
            }
        },
    )
}

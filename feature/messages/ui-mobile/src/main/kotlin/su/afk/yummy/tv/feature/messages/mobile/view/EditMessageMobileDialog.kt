package su.afk.yummy.tv.feature.messages.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.messages.presentation.R

@Composable
internal fun EditMessageMobileDialog(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text(stringResource(R.string.messages_edit_title)) },
        confirmText = stringResource(R.string.messages_save),
        onConfirm = onConfirm,
        confirmEnabled = enabled && text.isNotBlank(),
        dismissText = stringResource(CoreR.string.common_cancel),
        onDismiss = onDismiss,
        dismissEnabled = enabled,
        content = {
            Column(modifier = Modifier.imePadding()) {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = text,
                    onValueChange = onTextChange,
                    enabled = enabled,
                    minLines = 3,
                    maxLines = 8,
                )
            }
        },
    )
}

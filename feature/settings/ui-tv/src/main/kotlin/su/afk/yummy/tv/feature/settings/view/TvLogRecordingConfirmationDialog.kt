package su.afk.yummy.tv.feature.settings.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import su.afk.yummy.tv.core.designsystem.focus.TvDialogButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.core.designsystem.focus.tvBackKeyForwarding

@Composable
internal fun TvLogRecordingConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.tvBackKeyForwarding(),
        title = { Text(stringResource(R.string.settings_log_recording_confirm_title)) },
        text = { Text(stringResource(R.string.settings_log_recording_confirm_message)) },
        confirmButton = {
            TvDialogButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_log_recording_confirm))
            }
        },
        dismissButton = {
            TvDialogButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocusRequester),
            ) {
                Text(stringResource(CoreR.string.common_cancel))
            }
        },
    )
}

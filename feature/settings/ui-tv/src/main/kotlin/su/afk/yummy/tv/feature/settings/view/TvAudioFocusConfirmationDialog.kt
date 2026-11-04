package su.afk.yummy.tv.feature.settings.view

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
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.settings.presentation.R

/** Предупреждение перед отключением аудиофокуса плеера. */
@Composable
internal fun TvAudioFocusConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_audio_focus_confirm_title)) },
        text = { Text(stringResource(R.string.settings_audio_focus_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_audio_focus_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocusRequester),
            ) {
                Text(stringResource(CoreR.string.common_cancel))
            }
        },
    )
}

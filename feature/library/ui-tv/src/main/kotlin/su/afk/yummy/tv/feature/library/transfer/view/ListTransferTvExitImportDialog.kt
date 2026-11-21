package su.afk.yummy.tv.feature.library.transfer.view

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
import su.afk.yummy.tv.core.designsystem.focus.tvBackKeyForwarding
import su.afk.yummy.tv.feature.library.presentation.R

@Composable
internal fun ListTransferTvExitImportDialog(
    onContinue: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    val continueFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { continueFocusRequester.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onContinue,
        modifier = Modifier.tvBackKeyForwarding(),
        title = { Text(stringResource(R.string.library_transfer_exit_import_title)) },
        text = { Text(stringResource(R.string.library_transfer_exit_import_message)) },
        confirmButton = {
            TvDialogButton(onClick = onConfirmExit, destructive = true) {
                Text(text = stringResource(R.string.library_transfer_exit_import_confirm))
            }
        },
        dismissButton = {
            TvDialogButton(
                onClick = onContinue,
                modifier = Modifier.focusRequester(continueFocusRequester),
            ) {
                Text(stringResource(R.string.library_transfer_exit_import_continue))
            }
        },
    )
}

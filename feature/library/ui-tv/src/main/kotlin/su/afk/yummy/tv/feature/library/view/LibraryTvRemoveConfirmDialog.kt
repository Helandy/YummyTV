package su.afk.yummy.tv.feature.library.view

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
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.feature.library.presentation.R

/** Подтверждение удаления тайтла из списка Библиотеки на ТВ; по умолчанию фокус на «Удалить». */
@Composable
internal fun LibraryTvRemoveConfirmDialog(
    title: String,
    listTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { confirmFocusRequester.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_mobile_remove_title)) },
        text = { Text(stringResource(R.string.library_mobile_remove_message, title, listTitle)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.focusRequester(confirmFocusRequester),
            ) {
                Text(
                    text = stringResource(CoreR.string.common_delete),
                    color = YummySemanticColors.Destructive,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.common_cancel))
            }
        },
    )
}

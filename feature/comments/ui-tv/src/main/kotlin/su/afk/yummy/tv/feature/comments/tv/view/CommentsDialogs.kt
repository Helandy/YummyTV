package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.feature.comments.tv.R
import su.afk.yummy.tv.feature.comments.tv.utils.labelRes

@Composable
internal fun CommentsDialogs(
    pendingDelete: Comment?,
    pendingReport: Comment?,
    isMutating: Boolean,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onReportConfirmed: (CommentReportReason) -> Unit,
    onReportDismissed: () -> Unit,
) {
    if (pendingDelete != null) {
        val confirmFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { confirmFocusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { if (!isMutating) onDeleteDismissed() },
            title = { Text(stringResource(R.string.comments_delete_title)) },
            text = { Text(stringResource(R.string.comments_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirmed,
                    enabled = !isMutating,
                    modifier = Modifier.focusRequester(confirmFocusRequester),
                ) {
                    Text(stringResource(R.string.comments_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed, enabled = !isMutating) {
                    Text(stringResource(R.string.comments_cancel))
                }
            },
        )
    }
    if (pendingReport != null) {
        val firstReasonFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { firstReasonFocusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { if (!isMutating) onReportDismissed() },
            title = { Text(stringResource(R.string.comments_report_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.comments_report_message))
                    CommentReportReason.entries.forEachIndexed { index, reason ->
                        TextButton(
                            onClick = { onReportConfirmed(reason) },
                            enabled = !isMutating,
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstReasonFocusRequester)
                            } else {
                                Modifier
                            },
                        ) {
                            Text(stringResource(reason.labelRes()))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onReportDismissed, enabled = !isMutating) {
                    Text(stringResource(R.string.comments_cancel))
                }
            },
        )
    }
}

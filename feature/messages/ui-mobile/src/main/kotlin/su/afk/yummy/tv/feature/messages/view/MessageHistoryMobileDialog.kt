package su.afk.yummy.tv.feature.messages.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.messages.model.MessageHistoryChangeType
import su.afk.yummy.tv.domain.messages.model.MessageHistoryEntry
import su.afk.yummy.tv.feature.messages.mobile.R
import su.afk.yummy.tv.feature.messages.mobile.utils.formatMessageDate

@Composable
internal fun MessageHistoryMobileDialog(
    entries: List<MessageHistoryEntry>,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.messages_history_title)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 480.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    hasError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.messages_history_error))
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.messages_retry)) }
                    }

                    entries.isEmpty() -> Text(stringResource(R.string.messages_history_empty))
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(entries) { index, entry ->
                            MessageHistoryRow(entry)
                            if (index < entries.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.messages_close)) }
        },
    )
}

@Composable
private fun MessageHistoryRow(entry: MessageHistoryEntry) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = entry.changeType.label(),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = buildString {
                append(entry.nickname.ifBlank {
                    context.getString(
                        R.string.messages_unknown_user,
                        entry.userId
                    )
                })
                if (entry.dateSeconds > 0) {
                    append(" · ")
                    append(entry.dateSeconds.formatMessageDate(context))
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (entry.oldText.isNotBlank()) {
            Text(stringResource(R.string.messages_history_old_text, entry.oldText))
        }
        if (entry.newText.isNotBlank()) {
            Text(stringResource(R.string.messages_history_new_text, entry.newText))
        }
    }
}

@Composable
private fun MessageHistoryChangeType.label(): String = stringResource(
    when (this) {
        MessageHistoryChangeType.ADD -> R.string.messages_history_added
        MessageHistoryChangeType.DELETE -> R.string.messages_history_deleted
        MessageHistoryChangeType.EDIT -> R.string.messages_history_edited
        MessageHistoryChangeType.RESTORE -> R.string.messages_history_restored
        MessageHistoryChangeType.UNKNOWN -> R.string.messages_history_changed
    }
)

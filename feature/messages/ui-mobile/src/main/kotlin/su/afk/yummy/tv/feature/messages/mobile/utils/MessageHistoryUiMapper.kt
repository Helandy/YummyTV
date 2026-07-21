package su.afk.yummy.tv.feature.messages.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.messages.model.MessageHistoryChangeType
import su.afk.yummy.tv.feature.messages.mobile.R

@Composable
internal fun MessageHistoryChangeType.label(): String = stringResource(
    when (this) {
        MessageHistoryChangeType.ADD -> R.string.messages_history_added
        MessageHistoryChangeType.DELETE -> R.string.messages_history_deleted
        MessageHistoryChangeType.EDIT -> R.string.messages_history_edited
        MessageHistoryChangeType.RESTORE -> R.string.messages_history_restored
        MessageHistoryChangeType.UNKNOWN -> R.string.messages_history_changed
    }
)

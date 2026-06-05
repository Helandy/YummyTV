@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun NotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    contentFocusRequester: FocusRequester? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (contentFocusRequester != null) {
                    Modifier
                        .focusRequester(contentFocusRequester)
                        .focusable()
                } else {
                    Modifier
                },
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.STATS))
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isNotificationsLoading && state.notifications.isEmpty()) {
            EmptyText(stringResource(R.string.account_loading))
        } else if (state.notifications.isEmpty()) {
            EmptyText(stringResource(R.string.account_notifications_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = { onEvent(AccountState.Event.NotificationSelected(notification.id)) },
                        onRead = { onEvent(AccountState.Event.NotificationReadSelected(notification.id)) },
                        onDelete = { onEvent(AccountState.Event.NotificationDeleteSelected(notification.id)) },
                    )
                }
            }
        }
    }
}

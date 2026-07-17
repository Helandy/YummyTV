package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileNotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
) {
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    val unreadCount = state.unreadNotificationCount
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AccountMobileNotificationTypeBadges(state.unreadNotificationCounts)
        if (unreadCount > 0 || state.notifications.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (unreadCount > 0) {
                    OutlinedButton(
                        onClick = { onEvent(AccountState.Event.AllNotificationsReadSelected) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isNotificationsLoading,
                    ) {
                        Text(stringResource(R.string.account_mark_all_read))
                    }
                }
                if (state.notifications.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showDeleteAllConfirm = true },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isNotificationsLoading,
                    ) {
                        Text(stringResource(R.string.account_delete_all_notifications))
                    }
                }
            }
        }
        when {
            state.isNotificationsLoading && state.notifications.isEmpty() -> {
                AccountMobileLoadingIndicator()
            }

            state.notifications.isEmpty() -> {
                AccountMobileEmptyText(stringResource(R.string.account_notifications_empty))
            }

            else -> {
                state.notifications.forEach { notification ->
                    AccountMobileNotificationRow(
                        notification = notification,
                        onClick = { onEvent(AccountState.Event.NotificationSelected(notification.id)) },
                        onRead = { onEvent(AccountState.Event.NotificationReadSelected(notification.id)) },
                        onDelete = {
                            onEvent(
                                AccountState.Event.NotificationDeleteSelected(
                                    notification.id
                                )
                            )
                        },
                    )
                }
            }
        }
    }
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.account_delete_all_notifications_title)) },
            text = { Text(stringResource(R.string.account_delete_all_notifications_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllConfirm = false
                    onEvent(AccountState.Event.AllNotificationsDeleteSelected)
                }) { Text(stringResource(R.string.account_delete_all_notifications)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text(stringResource(R.string.account_cancel))
                }
            },
        )
    }
}

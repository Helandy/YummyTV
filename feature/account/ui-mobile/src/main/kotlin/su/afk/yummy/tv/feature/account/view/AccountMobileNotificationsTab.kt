package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileNotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
) {
    val unreadCount = state.notificationCounts.sumOf { it.count }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (unreadCount > 0) {
            OutlinedButton(
                onClick = { onEvent(AccountState.Event.AllNotificationsReadSelected) },
                enabled = !state.isNotificationsLoading,
            ) {
                Text(stringResource(R.string.account_mark_all_read))
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
}

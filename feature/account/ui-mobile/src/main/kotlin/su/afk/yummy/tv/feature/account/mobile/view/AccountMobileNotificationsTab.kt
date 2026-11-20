package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.MobileAlertDialog
import su.afk.yummy.tv.feature.account.notifications.NotificationsState
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.common.utils.isEmpty

@Composable
internal fun AccountMobileNotificationsTab(
    state: NotificationsState.State,
    onEvent: (NotificationsState.Event) -> Unit,
    /** Пользователь открыл уведомление или ссылку из него: экран держит лоадер до перехода. */
    onOpenNotification: () -> Unit,
) {
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showReadAllConfirm by remember { mutableStateOf(false) }
    val unreadCount = state.unreadNotificationCount
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AccountMobileMySubscriptionsRow(
            onClick = { onEvent(NotificationsState.Event.MySubscriptionsSelected) },
        )
        AccountMobileNotificationTypeBadges(state.unreadNotificationCounts)
        if (unreadCount > 0 || state.notifications.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (unreadCount > 0) {
                    TextButton(
                        onClick = { showReadAllConfirm = true },
                        modifier = Modifier.weight(1f, fill = false),
                        enabled = !state.isNotificationsLoading,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(R.string.account_mark_all_read),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (state.notifications.isNotEmpty()) {
                    TextButton(
                        onClick = { showDeleteAllConfirm = true },
                        modifier = Modifier.weight(1f, fill = false),
                        enabled = !state.isNotificationsLoading,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(R.string.account_delete_all_notifications),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                        onClick = {
                            onOpenNotification()
                            onEvent(NotificationsState.Event.NotificationSelected(notification.id))
                        },
                        onRead = { onEvent(NotificationsState.Event.NotificationReadSelected(notification.id)) },
                        expanded = notification.id in state.expandedNotificationIds,
                        onToggleExpand = {
                            onEvent(NotificationsState.Event.NotificationExpandToggled(notification.id))
                        },
                        onLinkClick = { slug ->
                            onOpenNotification()
                            onEvent(NotificationsState.Event.NotificationLinkSelected(notification.id, slug))
                        },
                        onDelete = {
                            onEvent(
                                NotificationsState.Event.NotificationDeleteSelected(
                                    notification.id
                                )
                            )
                        },
                    )
                }
                if (state.isNotificationsLoadingMore) {
                    AccountMobileLoadingIndicator()
                } else if (state.hasMoreNotifications) {
                    TextButton(
                        onClick = { onEvent(NotificationsState.Event.NotificationsLoadMoreSelected) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.account_notifications_load_more))
                    }
                }
            }
        }
    }
    if (showReadAllConfirm) {
        MobileAlertDialog(
            onDismissRequest = { showReadAllConfirm = false },
            title = { Text(stringResource(R.string.account_mark_all_read_title)) },
            confirmText = stringResource(R.string.account_mark_all_read),
            onConfirm = {
                    showReadAllConfirm = false
                    onEvent(NotificationsState.Event.AllNotificationsReadSelected)
                },
            dismissText = stringResource(CoreR.string.common_cancel),
            onDismiss = { showReadAllConfirm = false },
            content = { Text(stringResource(R.string.account_mark_all_read_message)) },
        )
    }
    if (showDeleteAllConfirm) {
        MobileAlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.account_delete_all_notifications_title)) },
            confirmText = stringResource(R.string.account_delete_all_notifications),
            onConfirm = {
                    showDeleteAllConfirm = false
                    onEvent(NotificationsState.Event.AllNotificationsDeleteSelected)
                },
            dismissText = stringResource(CoreR.string.common_cancel),
            onDismiss = { showDeleteAllConfirm = false },
            content = { Text(stringResource(R.string.account_delete_all_notifications_message)) },
        )
    }
}

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
import androidx.compose.material3.AlertDialog
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
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateHost
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileNotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
) {
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showReadAllConfirm by remember { mutableStateOf(false) }
    val unreadCount = state.unreadNotificationCount
    val notificationPermissionGate = rememberNotificationPermissionGate()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AccountMobileEpisodePushRow(
            enabled = state.episodePushEnabled,
            onClick = {
                if (state.episodePushEnabled) {
                    onEvent(AccountState.Event.EpisodePushToggled)
                } else {
                    notificationPermissionGate { onEvent(AccountState.Event.EpisodePushToggled) }
                }
            },
        )
        AccountMobileMySubscriptionsRow(
            onClick = { onEvent(AccountState.Event.MySubscriptionsSelected) },
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
    if (showReadAllConfirm) {
        AlertDialog(
            onDismissRequest = { showReadAllConfirm = false },
            title = { Text(stringResource(R.string.account_mark_all_read_title)) },
            text = { Text(stringResource(R.string.account_mark_all_read_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showReadAllConfirm = false
                    onEvent(AccountState.Event.AllNotificationsReadSelected)
                }) { Text(stringResource(R.string.account_mark_all_read)) }
            },
            dismissButton = {
                TextButton(onClick = { showReadAllConfirm = false }) {
                    Text(stringResource(R.string.account_cancel))
                }
            },
        )
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
    NotificationPermissionGateHost(state = notificationPermissionGate)
}

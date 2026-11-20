@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import su.afk.yummy.tv.feature.account.account.model.rememberNotificationsTabState
import su.afk.yummy.tv.feature.account.account.model.NotificationsTabState
import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.notifications.NotificationsState
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.common.utils.isEmpty
import su.afk.yummy.tv.feature.account.common.utils.label
import su.afk.yummy.tv.core.designsystem.focus.tvBackKeyForwarding

@Composable
internal fun NotificationsTab(
    state: AccountState.State,
    notificationsState: NotificationsState.State,
    onEvent: (AccountState.Event) -> Unit,
    onNotificationsEvent: (NotificationsState.Event) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    isActiveDestination: Boolean = true,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val notificationsTabState = rememberNotificationsTabState()
    val notificationIds = remember(notificationsState.notifications) { notificationsState.notifications.map { it.id } }
    val notificationReadStates =
        remember(notificationsState.notifications) { notificationsState.notifications.map { it.viewed } }
    val showOpeningOverlay =
        notificationsState.isNotificationOpening || notificationsTabState.showOpeningOverlayImmediately
    val notificationsErrorWithoutOpenFailure =
        notificationsState.error.takeIf { it != AccountUiError.OPEN_NOTIFICATION_FAILED }
    val context = LocalContext.current
    val openNotificationFailed = AccountUiError.OPEN_NOTIFICATION_FAILED.accountErrorMessage()
    LaunchedEffect(notificationsState.error) {
        if (notificationsState.error == AccountUiError.OPEN_NOTIFICATION_FAILED && openNotificationFailed != null) {
            Toast.makeText(context, openNotificationFailed, Toast.LENGTH_SHORT).show()
        }
    }

    NotificationsTabEffects(
        notificationsTabState = notificationsTabState,
        notificationIds = notificationIds,
        notificationReadStates = notificationReadStates,
        isActiveDestination = isActiveDestination,
        isNotificationsLoading = notificationsState.isNotificationsLoading,
        isNotificationOpening = notificationsState.isNotificationOpening,
        showOpeningOverlay = showOpeningOverlay,
        hubError = notificationsState.error,
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = notificationsTabState.listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .tvFocusRestorer(
                    fallback = notificationsTabState.notificationListFallbackFocusRequester(
                        notificationIds = notificationIds,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                    ),
                ),
        ) {
            item {
                AccountHeader(
                    state = state,
                    onEvent = onEvent,
                    downFocusRequester = selectedTabFocusRequester,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountTabs(
                        selected = state.selectedTab,
                        onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                        unreadCount = state.unreadNotificationCount,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        // Вниз с таба — на «Мои подписки»: дальше по колонке фокус ходит сам.
                        contentFocusRequester = notificationsTabState.focusRequesters.mySubscriptionsFocusRequester,
                        onContentRequested = {
                            notificationsTabState.requestFocusSafely(
                                notificationsTabState.focusRequesters.mySubscriptionsFocusRequester
                            )
                        },
                        autoFocusSelected = !notificationsTabState.notificationContentHasFocus &&
                                !notificationsTabState.suppressNotificationFocusUpdates,
                    )
                    AccountAction(
                        label = stringResource(R.string.account_my_subscriptions),
                        icon = Icons.Filled.Subscriptions,
                        onClick = { onNotificationsEvent(NotificationsState.Event.MySubscriptionsSelected) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(notificationsTabState.focusRequesters.mySubscriptionsFocusRequester),
                    )
                    if (notificationsState.notificationCounts.any { it.count > 0 } || notificationsState.notifications.isNotEmpty()) {
                        NotificationTypeBadges(notificationsState.notificationCounts)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (onMarkAllRead != null) {
                                AccountAction(
                                    label = stringResource(R.string.account_mark_all_read),
                                    icon = Icons.Filled.DoneAll,
                                    onClick = { notificationsTabState.showReadAllConfirm = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(notificationsTabState.focusRequesters.markAllReadFocusRequester)
                                        .focusProperties {
                                            if (notificationsState.notifications.isNotEmpty()) {
                                                right =
                                                    notificationsTabState.focusRequesters.deleteAllFocusRequester
                                            }
                                        },
                                    enabled = markAllReadEnabled,
                                )
                            }
                            if (notificationsState.notifications.isNotEmpty()) {
                                AccountAction(
                                    label = stringResource(R.string.account_delete_all_notifications),
                                    icon = Icons.Filled.DeleteSweep,
                                    onClick = { notificationsTabState.showDeleteAllConfirm = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(notificationsTabState.focusRequesters.deleteAllFocusRequester)
                                        .focusProperties {
                                            if (onMarkAllRead != null) {
                                                left =
                                                    notificationsTabState.focusRequesters.markAllReadFocusRequester
                                            }
                                        },
                                    enabled = !notificationsState.isNotificationsLoading,
                                )
                            }
                        }
                    }
                }
            }
            item {
                // Ошибку открытия уведомления показываем тостом, а не блоком с «Повторить».
                AccountHubError(
                    error = (state.error ?: notificationsErrorWithoutOpenFailure).accountErrorMessage(),
                    onRetry = {
                        onEvent(AccountState.Event.RefreshHubSelected)
                        onNotificationsEvent(NotificationsState.Event.RefreshSelected)
                    },
                )
            }
            if (notificationsState.isNotificationsLoading && notificationsState.notifications.isEmpty()) {
                item {
                    TvLoadingScreen(
                        modifier = Modifier.height(360.dp),
                    )
                }
            } else if (notificationsState.notifications.isEmpty()) {
                item {
                    // Не фокусируется: подсветки у текста нет, фокус в нём был бы невидим.
                    EmptyText(stringResource(R.string.account_notifications_empty))
                }
            } else {
                itemsIndexed(
                    notificationsState.notifications,
                    key = { index, item -> lazyKey("notification", item.id, index) },
                ) { index, notification ->
                    NotificationsTabRow(
                        notificationsTabState = notificationsTabState,
                        notificationIds = notificationIds,
                        index = index,
                        notification = notification,
                        expanded = notification.id in notificationsState.expandedNotificationIds,
                        mainMenuFocusRequester = mainMenuFocusRequester,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        onEvent = onNotificationsEvent,
                    )
                }
            }
        }
        if (showOpeningOverlay) {
            TvLoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(notificationsTabState.focusRequesters.openingOverlayFocusRequester)
                    .onPreviewKeyEvent { true }
                    .focusable(),
            )
        }
    }

    if (notificationsTabState.showReadAllConfirm) {
        AlertDialog(
            onDismissRequest = { notificationsTabState.showReadAllConfirm = false },
            modifier = Modifier.tvBackKeyForwarding(),
            title = { Text(stringResource(R.string.account_mark_all_read_title)) },
            text = { Text(stringResource(R.string.account_mark_all_read_message)) },
            confirmButton = {
                TextButton(onClick = {
                    notificationsTabState.showReadAllConfirm = false
                    onMarkAllRead?.invoke()
                }) { Text(stringResource(R.string.account_mark_all_read)) }
            },
            dismissButton = {
                TextButton(onClick = { notificationsTabState.showReadAllConfirm = false }) {
                    Text(stringResource(CoreR.string.common_cancel))
                }
            },
        )
    }

    if (notificationsTabState.showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { notificationsTabState.showDeleteAllConfirm = false },
            modifier = Modifier.tvBackKeyForwarding(),
            title = { Text(stringResource(R.string.account_delete_all_notifications_title)) },
            text = { Text(stringResource(R.string.account_delete_all_notifications_message)) },
            confirmButton = {
                TextButton(onClick = {
                    notificationsTabState.showDeleteAllConfirm = false
                    onNotificationsEvent(NotificationsState.Event.AllNotificationsDeleteSelected)
                }) { Text(stringResource(R.string.account_delete_all_notifications)) }
            },
            dismissButton = {
                TextButton(onClick = { notificationsTabState.showDeleteAllConfirm = false }) {
                    Text(stringResource(CoreR.string.common_cancel))
                }
            },
        )
    }
}

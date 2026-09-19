@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage

@Composable
internal fun NotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    isActiveDestination: Boolean = true,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val notificationsTabState = rememberNotificationsTabState()
    val notificationIds = remember(state.notifications) { state.notifications.map { it.id } }
    val notificationReadStates =
        remember(state.notifications) { state.notifications.map { it.viewed } }
    val showOpeningOverlay =
        state.isNotificationOpening || notificationsTabState.showOpeningOverlayImmediately

    NotificationsTabEffects(
        notificationsTabState = notificationsTabState,
        notificationIds = notificationIds,
        notificationReadStates = notificationReadStates,
        isActiveDestination = isActiveDestination,
        isNotificationsLoading = state.isNotificationsLoading,
        isNotificationOpening = state.isNotificationOpening,
        showOpeningOverlay = showOpeningOverlay,
        hubError = state.hubError,
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
                        contentFocusRequester = notificationsTabState.mySubscriptionsFocusRequester,
                        onContentRequested = {
                            notificationsTabState.requestFocusSafely(
                                notificationsTabState.mySubscriptionsFocusRequester
                            )
                        },
                        autoFocusSelected = !notificationsTabState.notificationContentHasFocus &&
                                !notificationsTabState.suppressNotificationFocusUpdates,
                    )
                    AccountAction(
                        label = stringResource(R.string.account_my_subscriptions),
                        icon = Icons.Filled.Subscriptions,
                        onClick = { onEvent(AccountState.Event.MySubscriptionsSelected) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(notificationsTabState.mySubscriptionsFocusRequester),
                    )
                    if (state.notificationCounts.any { it.count > 0 } || state.notifications.isNotEmpty()) {
                        NotificationTypeBadges(state.notificationCounts)
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
                                        .focusRequester(notificationsTabState.markAllReadFocusRequester)
                                        .focusProperties {
                                            if (state.notifications.isNotEmpty()) {
                                                right =
                                                    notificationsTabState.deleteAllFocusRequester
                                            }
                                        },
                                    enabled = markAllReadEnabled,
                                )
                            }
                            if (state.notifications.isNotEmpty()) {
                                AccountAction(
                                    label = stringResource(R.string.account_delete_all_notifications),
                                    icon = Icons.Filled.DeleteSweep,
                                    onClick = { notificationsTabState.showDeleteAllConfirm = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(notificationsTabState.deleteAllFocusRequester)
                                        .focusProperties {
                                            if (onMarkAllRead != null) {
                                                left =
                                                    notificationsTabState.markAllReadFocusRequester
                                            }
                                        },
                                    enabled = !state.isNotificationsLoading,
                                )
                            }
                        }
                    }
                }
            }
            item {
                AccountHubError(
                    error = (state.error ?: state.hubError).accountErrorMessage(),
                    onRetry = { onEvent(AccountState.Event.RefreshHubSelected) },
                )
            }
            if (state.isNotificationsLoading && state.notifications.isEmpty()) {
                item {
                    TvLoadingScreen(
                        modifier = Modifier.height(360.dp),
                    )
                }
            } else if (state.notifications.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .focusRequester(notificationsTabState.contentFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    notificationsTabState.markContentFocused()
                                }
                            }
                            .focusable()
                            .onPreviewKeyEvent { event ->
                                if (
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionLeft
                                ) {
                                    onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.STATS))
                                    true
                                } else if (
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionUp
                                ) {
                                    notificationsTabState.requestSelectedTabFocus(
                                        selectedTabFocusRequester
                                    )
                                } else {
                                    false
                                }
                            },
                    ) {
                        EmptyText(stringResource(R.string.account_notifications_empty))
                    }
                }
            } else {
                itemsIndexed(
                    state.notifications,
                    key = { _, item -> item.id },
                ) { index, notification ->
                    NotificationsTabRow(
                        notificationsTabState = notificationsTabState,
                        notificationIds = notificationIds,
                        index = index,
                        notification = notification,
                        mainMenuFocusRequester = mainMenuFocusRequester,
                        selectedTabFocusRequester = selectedTabFocusRequester,
                        onEvent = onEvent,
                    )
                }
            }
        }
        if (showOpeningOverlay) {
            TvLoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(notificationsTabState.openingOverlayFocusRequester)
                    .onPreviewKeyEvent { true }
                    .focusable(),
            )
        }
    }

    if (notificationsTabState.showReadAllConfirm) {
        AlertDialog(
            onDismissRequest = { notificationsTabState.showReadAllConfirm = false },
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
                    Text(stringResource(R.string.account_cancel))
                }
            },
        )
    }

    if (notificationsTabState.showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { notificationsTabState.showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.account_delete_all_notifications_title)) },
            text = { Text(stringResource(R.string.account_delete_all_notifications_message)) },
            confirmButton = {
                TextButton(onClick = {
                    notificationsTabState.showDeleteAllConfirm = false
                    onEvent(AccountState.Event.AllNotificationsDeleteSelected)
                }) { Text(stringResource(R.string.account_delete_all_notifications)) }
            },
            dismissButton = {
                TextButton(onClick = { notificationsTabState.showDeleteAllConfirm = false }) {
                    Text(stringResource(R.string.account_cancel))
                }
            },
        )
    }
}

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun NotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    contentFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val notificationIds = remember(state.notifications) { state.notifications.map { it.id } }
    val rowFocusRequesters = remember(notificationIds) {
        List(notificationIds.size) { FocusRequester() }
    }
    val readFocusRequesters = remember(notificationIds) {
        List(notificationIds.size) { FocusRequester() }
    }
    val deleteFocusRequesters = remember(notificationIds) {
        List(notificationIds.size) { FocusRequester() }
    }
    val fallbackContentFocusRequester = remember { FocusRequester() }
    val emptyContentFocusRequester = contentFocusRequester ?: fallbackContentFocusRequester
    val focusedNotificationIndex =
        state.focusedNotificationId?.let { id -> state.notifications.indexOfFirst { it.id == id } }
            ?: -1
    var lastFocusedIndex by rememberSaveable {
        mutableIntStateOf(
            focusedNotificationIndex.coerceAtLeast(
                0
            )
        )
    }
    var restoreFocusedNotificationToken by rememberSaveable { mutableIntStateOf(0) }
    var focusMoveJob by remember { mutableStateOf<Job?>(null) }
    val currentRestoreFocusedNotificationOnEnter by rememberUpdatedState(state.restoreFocusedNotificationOnEnter)
    val currentFocusedNotificationIndex by rememberUpdatedState(focusedNotificationIndex)
    val currentFocusedNotificationRestoreToken by rememberUpdatedState(state.focusedNotificationRestoreToken)
    var handledRestoreToken by rememberSaveable { mutableIntStateOf(0) }

    fun notificationRowFocusRequester(index: Int): FocusRequester =
        if (index == 0 && contentFocusRequester != null) {
            contentFocusRequester
        } else {
            rowFocusRequesters[index]
        }

    fun requestNotificationFocus(index: Int) {
        if (index !in state.notifications.indices) return
        focusMoveJob?.cancel()
        lastFocusedIndex = index
        val rowFocusRequester = notificationRowFocusRequester(index)
        if (listState.layoutInfo.visibleItemsInfo.any { it.index == index }) {
            runCatching { rowFocusRequester.requestFocus() }
        } else {
            focusMoveJob = scope.launch {
                listState.scrollToItem(index)
                withFrameNanos { }
                runCatching { rowFocusRequester.requestFocus() }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                currentRestoreFocusedNotificationOnEnter &&
                currentFocusedNotificationRestoreToken > handledRestoreToken &&
                currentFocusedNotificationIndex >= 0
            ) {
                restoreFocusedNotificationToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.focusedNotificationId, notificationIds) {
        if (focusedNotificationIndex >= 0) {
            lastFocusedIndex = focusedNotificationIndex
        }
    }

    LaunchedEffect(
        restoreFocusedNotificationToken,
        notificationIds,
    ) {
        if (
            restoreFocusedNotificationToken <= 0 ||
            !state.restoreFocusedNotificationOnEnter ||
            focusedNotificationIndex !in state.notifications.indices
        ) {
            return@LaunchedEffect
        }
        handledRestoreToken = state.focusedNotificationRestoreToken
        listState.scrollToItem(focusedNotificationIndex)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == focusedNotificationIndex }
        }.first { it }
        repeat(6) {
            runCatching { notificationRowFocusRequester(focusedNotificationIndex).requestFocus() }
            withFrameNanos { }
        }
        onEvent(AccountState.Event.NotificationFocusRestoreHandled)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (state.notifications.isEmpty() && contentFocusRequester != null) {
                    Modifier
                        .focusRequester(emptyContentFocusRequester)
                        .focusable()
                } else {
                    Modifier
                },
            )
            .onPreviewKeyEvent { event ->
                if (
                    state.notifications.isEmpty() &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft
                ) {
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
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    state.notifications,
                    key = { _, item -> item.id }) { index, notification ->
                    val rowFocusRequester = notificationRowFocusRequester(index)
                    val readFocusRequester = readFocusRequesters[index]
                    val deleteFocusRequester = deleteFocusRequesters[index]
                    val firstActionFocusRequester =
                        readFocusRequester.takeIf { !notification.viewed } ?: deleteFocusRequester
                    NotificationRow(
                        notification = notification,
                        onClick = {
                            lastFocusedIndex = index
                            onEvent(AccountState.Event.NotificationSelected(notification.id))
                        },
                        onRead = { onEvent(AccountState.Event.NotificationReadSelected(notification.id)) },
                        onDelete = { onEvent(AccountState.Event.NotificationDeleteSelected(notification.id)) },
                        modifier = Modifier
                            .focusRequester(rowFocusRequester)
                            .focusProperties {
                                upFocusRequester?.takeIf { index == 0 }?.let { up = it }
                                right = firstActionFocusRequester
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionRight -> {
                                        runCatching { firstActionFocusRequester.requestFocus() }
                                        true
                                    }

                                    Key.DirectionDown -> {
                                        requestNotificationFocus(index + 1)
                                        index < state.notifications.lastIndex
                                    }

                                    Key.DirectionUp -> {
                                        if (index == 0) {
                                            false
                                        } else {
                                            requestNotificationFocus(index - 1)
                                            true
                                        }
                                    }

                                    else -> false
                                }
                            }
                            .onFocusChanged {
                                if (it.isFocused) {
                                    lastFocusedIndex = index
                                }
                            },
                        readModifier = Modifier
                            .focusRequester(readFocusRequester)
                            .focusProperties {
                                left = rowFocusRequester
                                right = deleteFocusRequester
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        runCatching { rowFocusRequester.requestFocus() }
                                        true
                                    }

                                    Key.DirectionRight -> {
                                        runCatching { deleteFocusRequester.requestFocus() }
                                        true
                                    }

                                    else -> false
                                }
                            },
                        deleteModifier = Modifier
                            .focusRequester(deleteFocusRequester)
                            .focusProperties {
                                left = readFocusRequester.takeIf { !notification.viewed }
                                    ?: rowFocusRequester
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                                    return@onPreviewKeyEvent false
                                }
                                val target = readFocusRequester.takeIf { !notification.viewed }
                                    ?: rowFocusRequester
                                runCatching { target.requestFocus() }
                                true
                            },
                    )
                }
            }
        }
    }
}

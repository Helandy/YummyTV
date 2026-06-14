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
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage

@Composable
internal fun NotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val contentFocusRequester = remember { FocusRequester() }
    val notificationIds = remember(state.notifications) { state.notifications.map { it.id } }
    val notificationReadStates =
        remember(state.notifications) { state.notifications.map { it.viewed } }
    val rowFocusRequesters = remember(notificationIds) {
        List(notificationIds.size) { FocusRequester() }
    }
    val readFocusRequesters = remember(notificationIds) {
        List(notificationIds.size) { FocusRequester() }
    }
    val deleteFocusRequesters = remember(notificationIds) {
        List(notificationIds.size) { FocusRequester() }
    }
    val emptyContentFocusRequester = contentFocusRequester
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
    var pendingReadFocusId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingReadSawLoading by rememberSaveable { mutableStateOf(false) }
    var pendingDeletedNotificationId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingFocusAfterDeleteIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingDeleteSawLoading by rememberSaveable { mutableStateOf(false) }

    fun notificationRowFocusRequester(index: Int): FocusRequester =
        if (index == 0) contentFocusRequester else rowFocusRequesters[index]

    fun notificationItemIndex(index: Int): Int = NOTIFICATIONS_CONTENT_ITEM_INDEX + index

    fun notifyNotificationFocused(index: Int) {
        if (index !in state.notifications.indices) return
        lastFocusedIndex = index
        onEvent(AccountState.Event.NotificationFocused(state.notifications[index].id))
    }

    suspend fun requestFocusAfterScroll(index: Int, focusRequester: FocusRequester) {
        if (index !in state.notifications.indices) return
        lastFocusedIndex = index
        val itemIndex = notificationItemIndex(index)
        listState.scrollToItem(itemIndex)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }
        }.first { it }
        repeat(6) {
            runCatching { focusRequester.requestFocus() }
            withFrameNanos { }
        }
        listState.scrollToItem(itemIndex)
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
    }

    suspend fun requestEmptyFocusAfterScroll() {
        listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any {
                it.index == NOTIFICATIONS_CONTENT_ITEM_INDEX
            }
        }.first { it }
        repeat(6) {
            runCatching { emptyContentFocusRequester.requestFocus() }
            withFrameNanos { }
        }
        listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        withFrameNanos { }
        runCatching { emptyContentFocusRequester.requestFocus() }
    }

    fun requestFocusAtIndex(
        index: Int,
        focusRequester: FocusRequester,
        alignScroll: Boolean = false,
    ) {
        if (index !in state.notifications.indices) return
        focusMoveJob?.cancel()
        lastFocusedIndex = index
        val itemIndex = notificationItemIndex(index)
        if (!alignScroll && listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }) {
            runCatching { focusRequester.requestFocus() }
        } else {
            focusMoveJob = scope.launch {
                requestFocusAfterScroll(index, focusRequester)
            }
        }
    }

    fun requestNotificationFocus(index: Int): Boolean {
        if (index !in state.notifications.indices) return false
        notifyNotificationFocused(index)
        requestFocusAtIndex(
            index = index,
            focusRequester = notificationRowFocusRequester(index),
            alignScroll = true,
        )
        return true
    }

    fun requestDeleteFocus(index: Int) {
        val deleteFocusRequester = deleteFocusRequesters.getOrNull(index) ?: return
        if (index !in state.notifications.indices) return
        notifyNotificationFocused(index)
        requestFocusAtIndex(index, deleteFocusRequester)
    }

    fun requestEmptyOrTopFocus() {
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch {
            requestEmptyFocusAfterScroll()
        }
    }

    fun scrollFirstNotificationToTop() {
        if (state.notifications.isEmpty()) return
        if (listState.firstVisibleItemIndex >= NOTIFICATIONS_CONTENT_ITEM_INDEX) return
        scope.launch { listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX) }
    }

    fun requestContentFocusFromTabs() {
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch {
            if (state.notifications.isEmpty()) {
                requestEmptyFocusAfterScroll()
            } else {
                val targetIndex = 0
                notifyNotificationFocused(targetIndex)
                requestFocusAfterScroll(
                    index = targetIndex,
                    focusRequester = notificationRowFocusRequester(targetIndex),
                )
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
        val focusedItemIndex = notificationItemIndex(focusedNotificationIndex)
        listState.scrollToItem(focusedItemIndex)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == focusedItemIndex }
        }.first { it }
        repeat(6) {
            runCatching { notificationRowFocusRequester(focusedNotificationIndex).requestFocus() }
            withFrameNanos { }
        }
        onEvent(AccountState.Event.NotificationFocusRestoreHandled)
    }

    LaunchedEffect(state.isNotificationsLoading, pendingReadFocusId, pendingDeletedNotificationId) {
        if (state.isNotificationsLoading) {
            if (pendingReadFocusId != null) pendingReadSawLoading = true
            if (pendingDeletedNotificationId != null) pendingDeleteSawLoading = true
        }
    }

    LaunchedEffect(
        notificationIds,
        notificationReadStates,
        pendingReadFocusId,
        pendingReadSawLoading,
        state.isNotificationsLoading,
    ) {
        val pendingId = pendingReadFocusId ?: return@LaunchedEffect
        val targetIndex = state.notifications.indexOfFirst { it.id == pendingId }
        val notification = state.notifications.getOrNull(targetIndex)
        when {
            notification == null -> {
                if (pendingReadSawLoading && !state.isNotificationsLoading) {
                    pendingReadFocusId = null
                    pendingReadSawLoading = false
                }
            }

            notification.viewed -> {
                val deleteFocusRequester = deleteFocusRequesters.getOrNull(targetIndex)
                    ?: return@LaunchedEffect
                notifyNotificationFocused(targetIndex)
                requestFocusAfterScroll(targetIndex, deleteFocusRequester)
                pendingReadFocusId = null
                pendingReadSawLoading = false
            }

            pendingReadSawLoading && !state.isNotificationsLoading -> {
                pendingReadFocusId = null
                pendingReadSawLoading = false
            }
        }
    }

    LaunchedEffect(
        notificationIds,
        pendingDeletedNotificationId,
        pendingFocusAfterDeleteIndex,
        pendingDeleteSawLoading,
        state.isNotificationsLoading,
    ) {
        val deletedId = pendingDeletedNotificationId ?: return@LaunchedEffect
        val targetIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        if (state.notifications.any { it.id == deletedId }) {
            if (pendingDeleteSawLoading && !state.isNotificationsLoading) {
                pendingDeletedNotificationId = null
                pendingFocusAfterDeleteIndex = null
                pendingDeleteSawLoading = false
            }
            return@LaunchedEffect
        }

        if (state.notifications.isEmpty()) {
            focusMoveJob?.cancel()
            requestEmptyFocusAfterScroll()
        } else {
            val restoredIndex = targetIndex.coerceIn(0, state.notifications.lastIndex)
            notifyNotificationFocused(restoredIndex)
            requestFocusAfterScroll(
                index = restoredIndex,
                focusRequester = notificationRowFocusRequester(restoredIndex),
            )
        }
        pendingDeletedNotificationId = null
        pendingFocusAfterDeleteIndex = null
        pendingDeleteSawLoading = false
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            AccountHeader(state = state, onEvent = onEvent)
        }
        item {
            AccountTabs(
                selected = state.selectedTab,
                onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                selectedTabFocusRequester = selectedTabFocusRequester,
                contentFocusRequester = contentFocusRequester,
                onContentRequested = ::requestContentFocusFromTabs,
                onMarkAllRead = onMarkAllRead,
                markAllReadEnabled = markAllReadEnabled,
            )
        }
        item {
            ErrorText((state.error ?: state.hubError).accountErrorMessage())
        }
        if (state.isNotificationsLoading && state.notifications.isEmpty()) {
            item {
                EmptyText(stringResource(R.string.account_loading))
            }
        } else if (state.notifications.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .focusRequester(emptyContentFocusRequester)
                        .focusable()
                        .onFocusChanged {
                            if (it.isFocused) {
                                scope.launch {
                                    listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
                                }
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionLeft
                            ) {
                                onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.STATS))
                                true
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
                val rowFocusRequester = notificationRowFocusRequester(index)
                val readFocusRequester = readFocusRequesters[index]
                val deleteFocusRequester = deleteFocusRequesters[index]
                val firstActionFocusRequester =
                    readFocusRequester.takeIf { !notification.viewed } ?: deleteFocusRequester
                var rowIsFocused by remember(notification.id) { mutableStateOf(false) }
                NotificationRow(
                    notification = notification,
                    onClick = {
                        notifyNotificationFocused(index)
                        onEvent(AccountState.Event.NotificationSelected(notification.id))
                    },
                    onRead = {
                        pendingReadFocusId = notification.id
                        pendingReadSawLoading = false
                        requestDeleteFocus(index)
                        onEvent(AccountState.Event.NotificationReadSelected(notification.id))
                    },
                    onDelete = {
                        pendingDeletedNotificationId = notification.id
                        pendingFocusAfterDeleteIndex = index
                        pendingDeleteSawLoading = false
                        val immediateTarget =
                            if (index < state.notifications.lastIndex) index + 1 else index - 1
                        if (immediateTarget >= 0) {
                            requestNotificationFocus(immediateTarget)
                        } else {
                            requestEmptyOrTopFocus()
                        }
                        onEvent(AccountState.Event.NotificationDeleteSelected(notification.id))
                    },
                    onReadDirectionRight = {
                        requestDeleteFocus(index)
                        true
                    },
                    onDeleteDirectionLeft = {
                        val target = readFocusRequester.takeIf { !notification.viewed }
                            ?: rowFocusRequester
                        notifyNotificationFocused(index)
                        runCatching { target.requestFocus() }
                        true
                    },
                    modifier = Modifier
                        .focusRequester(rowFocusRequester)
                        .focusProperties {
                            selectedTabFocusRequester?.takeIf { index == 0 }?.let { up = it }
                            right = firstActionFocusRequester
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionRight -> {
                                    if (!rowIsFocused) return@onPreviewKeyEvent false
                                    if (notification.viewed) {
                                        requestDeleteFocus(index)
                                    } else {
                                        runCatching { firstActionFocusRequester.requestFocus() }
                                    }
                                    true
                                }

                                Key.DirectionDown -> {
                                    requestNotificationFocus(index + 1)
                                }

                                Key.DirectionUp -> {
                                    requestNotificationFocus(index - 1)
                                }

                                else -> false
                            }
                        }
                        .onFocusChanged {
                            rowIsFocused = it.isFocused
                            if (it.isFocused) {
                                notifyNotificationFocused(index)
                                if (index == 0) {
                                    scrollFirstNotificationToTop()
                                }
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
                                    requestDeleteFocus(index)
                                    true
                                }

                                Key.DirectionDown -> {
                                    requestNotificationFocus(index + 1)
                                }

                                Key.DirectionUp -> {
                                    requestNotificationFocus(index - 1)
                                }

                                else -> false
                            }
                        }
                        .onFocusChanged {
                            if (it.isFocused) {
                                notifyNotificationFocused(index)
                                if (index == 0) {
                                    scrollFirstNotificationToTop()
                                }
                            }
                        },
                    deleteModifier = Modifier
                        .focusRequester(deleteFocusRequester)
                        .focusProperties {
                            left = readFocusRequester.takeIf { !notification.viewed }
                                ?: rowFocusRequester
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    val target =
                                        readFocusRequester.takeIf { !notification.viewed }
                                            ?: rowFocusRequester
                                    runCatching { target.requestFocus() }
                                    true
                                }

                                Key.DirectionDown -> {
                                    requestNotificationFocus(index + 1)
                                }

                                Key.DirectionUp -> {
                                    requestNotificationFocus(index - 1)
                                }

                                else -> false
                            }
                        }
                        .onFocusChanged {
                            if (it.isFocused) {
                                notifyNotificationFocused(index)
                                if (index == 0) {
                                    scrollFirstNotificationToTop()
                                }
                            }
                        },
                )
            }
        }
    }
}

private const val NOTIFICATIONS_CONTENT_ITEM_INDEX = 3

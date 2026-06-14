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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    isActiveDestination: Boolean = true,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
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
    var focusMoveJob by remember { mutableStateOf<Job?>(null) }
    var handledRestoreToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreRequestedAfterInactiveDestination by rememberSaveable { mutableStateOf(false) }
    var pendingReadFocusId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingReadSawLoading by rememberSaveable { mutableStateOf(false) }
    var pendingDeletedNotificationId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingFocusAfterDeleteIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingDeleteSawLoading by rememberSaveable { mutableStateOf(false) }
    var notificationContentHasFocus by remember { mutableStateOf(false) }

    fun notificationRowFocusRequester(index: Int): FocusRequester =
        if (index == 0) contentFocusRequester else rowFocusRequesters[index]

    fun notificationItemIndex(index: Int): Int = NOTIFICATIONS_CONTENT_ITEM_INDEX + index

    fun previousVerticalFocusRequester(index: Int): FocusRequester? =
        when {
            index == 0 -> selectedTabFocusRequester
            index > 0 -> notificationRowFocusRequester(index - 1)
            else -> null
        }

    fun nextVerticalFocusRequester(index: Int): FocusRequester? =
        if (index < state.notifications.lastIndex) {
            notificationRowFocusRequester(index + 1)
        } else {
            null
        }

    fun requestFocusSafely(focusRequester: FocusRequester): Boolean =
        runCatching { focusRequester.requestFocus() }.getOrDefault(false)

    fun notifyNotificationFocused(index: Int) {
        if (index !in state.notifications.indices) return
        lastFocusedIndex = index
        onEvent(AccountState.Event.NotificationFocused(state.notifications[index].id))
    }

    suspend fun requestFocusAfterScroll(
        index: Int,
        focusRequester: FocusRequester,
        animateScroll: Boolean = false,
    ) {
        if (index !in state.notifications.indices) return
        lastFocusedIndex = index
        val itemIndex = notificationItemIndex(index)
        if (animateScroll) {
            listState.animateScrollToItem(itemIndex)
        } else {
            listState.scrollToItem(itemIndex)
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }
        }.first { it }
        repeat(6) {
            requestFocusSafely(focusRequester)
            withFrameNanos { }
        }
        if (!animateScroll) {
            listState.scrollToItem(itemIndex)
        }
        withFrameNanos { }
        requestFocusSafely(focusRequester)
    }

    suspend fun requestEmptyFocusAfterScroll(animateScroll: Boolean = false) {
        if (animateScroll) {
            listState.animateScrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        } else {
            listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any {
                it.index == NOTIFICATIONS_CONTENT_ITEM_INDEX
            }
        }.first { it }
        repeat(6) {
            requestFocusSafely(emptyContentFocusRequester)
            withFrameNanos { }
        }
        if (!animateScroll) {
            listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        }
        withFrameNanos { }
        requestFocusSafely(emptyContentFocusRequester)
    }

    suspend fun requestSelectedTabFocusAfterScroll(animateScroll: Boolean = false): Boolean {
        val selectedTabRequester = selectedTabFocusRequester ?: return false
        notificationContentHasFocus = false
        if (animateScroll) {
            listState.animateScrollToItem(NOTIFICATIONS_HEADER_ITEM_INDEX)
        } else {
            listState.scrollToItem(NOTIFICATIONS_HEADER_ITEM_INDEX)
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any {
                it.index == NOTIFICATIONS_TABS_ITEM_INDEX
            }
        }.first { it }
        repeat(6) {
            if (requestFocusSafely(selectedTabRequester)) return true
            withFrameNanos { }
        }
        return requestFocusSafely(selectedTabRequester)
    }

    fun requestSelectedTabFocus(): Boolean {
        val selectedTabRequester = selectedTabFocusRequester ?: return false
        notificationContentHasFocus = false
        val isHeaderAtTop = listState.firstVisibleItemIndex == NOTIFICATIONS_HEADER_ITEM_INDEX &&
                listState.firstVisibleItemScrollOffset == 0
        if (
            isHeaderAtTop &&
            listState.layoutInfo.visibleItemsInfo.any { it.index == NOTIFICATIONS_TABS_ITEM_INDEX } &&
            requestFocusSafely(selectedTabRequester)
        ) {
            return true
        }
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch { requestSelectedTabFocusAfterScroll(animateScroll = true) }
        return true
    }

    suspend fun requestFocusKeepingCurrentScroll(focusRequester: FocusRequester): Boolean {
        val firstVisibleIndex = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
        var focused = false
        repeat(4) {
            focused = requestFocusSafely(focusRequester) || focused
            withFrameNanos { }
            listState.scrollToItem(firstVisibleIndex, firstVisibleOffset)
            withFrameNanos { }
        }
        focused = requestFocusSafely(focusRequester) || focused
        withFrameNanos { }
        listState.scrollToItem(firstVisibleIndex, firstVisibleOffset)
        return focused
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
            requestFocusSafely(focusRequester)
        } else {
            focusMoveJob = scope.launch {
                requestFocusAfterScroll(
                    index = index,
                    focusRequester = focusRequester,
                    animateScroll = true,
                )
            }
        }
    }

    fun requestNotificationFocus(index: Int): Boolean {
        if (index !in state.notifications.indices) return false
        notifyNotificationFocused(index)
        requestFocusAtIndex(
            index = index,
            focusRequester = notificationRowFocusRequester(index),
        )
        return true
    }

    fun requestPreviousNotificationFocus(index: Int): Boolean {
        if (index > 0) return requestNotificationFocus(index - 1)
        return requestSelectedTabFocus()
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

    fun requestContentFocusFromTabs() {
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch {
            notificationContentHasFocus = true
            if (state.notifications.isEmpty()) {
                if (listState.layoutInfo.visibleItemsInfo.any { it.index == NOTIFICATIONS_CONTENT_ITEM_INDEX }) {
                    requestFocusKeepingCurrentScroll(emptyContentFocusRequester)
                } else {
                    requestEmptyFocusAfterScroll(animateScroll = true)
                }
            } else {
                val targetIndex = 0
                val itemIndex = notificationItemIndex(targetIndex)
                notifyNotificationFocused(targetIndex)
                if (listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }) {
                    requestFocusKeepingCurrentScroll(notificationRowFocusRequester(targetIndex))
                } else {
                    requestFocusAfterScroll(
                        index = targetIndex,
                        focusRequester = notificationRowFocusRequester(targetIndex),
                        animateScroll = true,
                    )
                }
            }
        }
    }

    LaunchedEffect(state.focusedNotificationId, notificationIds) {
        if (focusedNotificationIndex >= 0) {
            lastFocusedIndex = focusedNotificationIndex
        }
    }

    LaunchedEffect(
        isActiveDestination,
        state.restoreFocusedNotificationOnEnter,
        state.focusedNotificationId,
        state.focusedNotificationRestoreToken,
        notificationIds,
    ) {
        if (!isActiveDestination) {
            if (state.restoreFocusedNotificationOnEnter) {
                restoreRequestedAfterInactiveDestination = true
            }
            return@LaunchedEffect
        }
        if (!state.restoreFocusedNotificationOnEnter) {
            restoreRequestedAfterInactiveDestination = false
            return@LaunchedEffect
        }
        if (
            state.focusedNotificationRestoreToken <= handledRestoreToken ||
            !restoreRequestedAfterInactiveDestination ||
            focusedNotificationIndex !in state.notifications.indices
        ) {
            return@LaunchedEffect
        }
        restoreRequestedAfterInactiveDestination = false
        handledRestoreToken = state.focusedNotificationRestoreToken
        notificationContentHasFocus = true
        val restoreItemIndex = notificationItemIndex(focusedNotificationIndex)
        val restoreFocusRequester = notificationRowFocusRequester(focusedNotificationIndex)
        if (listState.layoutInfo.visibleItemsInfo.any { it.index == restoreItemIndex }) {
            requestFocusKeepingCurrentScroll(restoreFocusRequester)
        } else {
            requestFocusAfterScroll(
                index = focusedNotificationIndex,
                focusRequester = restoreFocusRequester,
            )
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
                autoFocusSelected = !notificationContentHasFocus &&
                        !state.restoreFocusedNotificationOnEnter,
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
                        .onFocusChanged {
                            if (it.isFocused) {
                                notificationContentHasFocus = true
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
                                requestSelectedTabFocus()
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
                        requestFocusSafely(target)
                        true
                    },
                    modifier = Modifier
                        .focusRequester(rowFocusRequester)
                        .focusProperties {
                            previousVerticalFocusRequester(index)?.let { up = it }
                            nextVerticalFocusRequester(index)?.let { down = it }
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
                                        requestFocusSafely(firstActionFocusRequester)
                                    }
                                    true
                                }

                                Key.DirectionDown -> {
                                    requestNotificationFocus(index + 1)
                                }

                                Key.DirectionUp -> {
                                    requestPreviousNotificationFocus(index)
                                }

                                else -> false
                            }
                        }
                        .onFocusChanged {
                            rowIsFocused = it.isFocused
                            if (it.isFocused) {
                                notificationContentHasFocus = true
                                notifyNotificationFocused(index)
                            }
                        },
                    readModifier = Modifier
                        .focusRequester(readFocusRequester)
                        .focusProperties {
                            left = rowFocusRequester
                            right = deleteFocusRequester
                            previousVerticalFocusRequester(index)?.let { up = it }
                            nextVerticalFocusRequester(index)?.let { down = it }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    requestFocusSafely(rowFocusRequester)
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
                                    requestPreviousNotificationFocus(index)
                                }

                                else -> false
                            }
                        }
                        .onFocusChanged {
                            if (it.isFocused) {
                                notificationContentHasFocus = true
                                notifyNotificationFocused(index)
                            }
                        },
                    deleteModifier = Modifier
                        .focusRequester(deleteFocusRequester)
                        .focusProperties {
                            left = readFocusRequester.takeIf { !notification.viewed }
                                ?: rowFocusRequester
                            previousVerticalFocusRequester(index)?.let { up = it }
                            nextVerticalFocusRequester(index)?.let { down = it }
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
                                    requestFocusSafely(target)
                                    true
                                }

                                Key.DirectionDown -> {
                                    requestNotificationFocus(index + 1)
                                }

                                Key.DirectionUp -> {
                                    requestPreviousNotificationFocus(index)
                                }

                                else -> false
                            }
                        }
                        .onFocusChanged {
                            if (it.isFocused) {
                                notificationContentHasFocus = true
                                notifyNotificationFocused(index)
                            }
                        },
                )
            }
        }
    }
}

private const val NOTIFICATIONS_TABS_ITEM_INDEX = 1
private const val NOTIFICATIONS_CONTENT_ITEM_INDEX = 3
private const val NOTIFICATIONS_HEADER_ITEM_INDEX = 0

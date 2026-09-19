package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.TvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.focus.launchTvLazyListKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.account.model.PendingNotificationDeleteFocus
import su.afk.yummy.tv.feature.account.account.model.PendingNotificationOpenRequest
import su.afk.yummy.tv.feature.account.account.model.PendingNotificationReadFocus

/**
 * Item order the [NotificationsTab] LazyColumn is built in: header=0, tabs+badges+actions=1,
 * hub error=2, content (loading/empty/rows) starts at 3. Scroll/visibility math below depends on
 * this - keep in sync with the LazyColumn body in NotificationsTab.kt if that order ever changes.
 */
private const val NOTIFICATIONS_HEADER_ITEM_INDEX = 0
private const val NOTIFICATIONS_TABS_ITEM_INDEX = 1
private const val NOTIFICATIONS_CONTENT_ITEM_INDEX = 3

/**
 * Owns D-pad focus routing/restoration for the TV notifications list: which row/action currently
 * has (or should get) focus, in-flight mark-read/delete/open operations waiting for their target
 * to reflect in [AccountState.State.notifications], and the scroll/focus mechanics to get there
 * without visible jank. Built via [rememberNotificationsTabState]; its reactive side effects live
 * in the paired [NotificationsTabEffects] composable.
 *
 * Never stores the notification list itself - callers pass the current `notificationIds` into
 * every method that needs list bounds or id lookups, so this class can't act on stale data.
 */
@Stable
internal class NotificationsTabState internal constructor(
    private val scope: CoroutineScope,
    val listState: LazyListState,
    val focusRestoreState: TvLazyFocusRestoreState<Int>,
    private val pendingOpenNotificationIdState: MutableState<Int?>,
    private val awaitingOpenReturnState: MutableState<Boolean>,
    private val pendingReadFocusIdState: MutableState<Int?>,
    private val pendingReadSawLoadingState: MutableState<Boolean>,
    private val pendingDeletedNotificationIdState: MutableState<Int?>,
    private val pendingDeleteFallbackIndexState: MutableState<Int?>,
    private val pendingDeleteSawLoadingState: MutableState<Boolean>,
) {
    /**
     * Triple role, intentional: passed to [AccountTabs] as its `contentFocusRequester` (Down/
     * Center/Enter on the tab focuses here), is row index 0's own focus requester, AND is the
     * empty-state placeholder's focus requester. Only one of the latter two is ever composed at
     * a time, so a single shared requester is safe and is what lets AccountTabs hand off focus
     * to "whatever the first thing in the list is" without knowing if the list is empty.
     */
    val contentFocusRequester = FocusRequester()

    /**
     * Кнопка «Мои подписки» — первый фокусируемый элемент под табами. Таб жёстко задаёт себе
     * `down`, поэтому без явного requester'а фокус перепрыгивал её и уходил сразу в список.
     */
    val mySubscriptionsFocusRequester = FocusRequester()
    val openingOverlayFocusRequester = FocusRequester()
    val markAllReadFocusRequester = FocusRequester()
    val deleteAllFocusRequester = FocusRequester()

    private val rowFocusRequesterCache = mutableMapOf<Int, FocusRequester>()
    private val readFocusRequesterCache = mutableMapOf<Int, FocusRequester>()
    private val deleteFocusRequesterCache = mutableMapOf<Int, FocusRequester>()

    var suppressNotificationFocusUpdates by mutableStateOf(false)
        private set
    var notificationContentHasFocus by mutableStateOf(false)
        private set
    var showOpeningOverlayImmediately by mutableStateOf(false)
        private set
    var showDeleteAllConfirm by mutableStateOf(false)
    var showReadAllConfirm by mutableStateOf(false)

    // Never read reactively (only cancelled/reassigned), so plain vars are enough - no need to
    // pay for snapshot-state tracking on these.
    private var focusMoveJob: Job? = null
    private var restoreFocusJob: Job? = null

    val openNotificationRequest: PendingNotificationOpenRequest?
        get() = pendingOpenNotificationIdState.value?.let {
            PendingNotificationOpenRequest(it, awaitingOpenReturnState.value)
        }
    val pendingReadFocus: PendingNotificationReadFocus?
        get() = pendingReadFocusIdState.value?.let {
            PendingNotificationReadFocus(it, pendingReadSawLoadingState.value)
        }
    val pendingDeleteFocus: PendingNotificationDeleteFocus?
        get() {
            val id = pendingDeletedNotificationIdState.value ?: return null
            val fallbackIndex = pendingDeleteFallbackIndexState.value ?: return null
            return PendingNotificationDeleteFocus(
                id,
                fallbackIndex,
                pendingDeleteSawLoadingState.value
            )
        }

    val shouldRestoreNotificationItemFocus: Boolean
        get() = notificationContentHasFocus ||
                openNotificationRequest != null ||
                suppressNotificationFocusUpdates

    // ---------------- pending-operation mutators ----------------

    /** Call when the user activates an openable notification row. */
    fun beginOpeningNotification(notificationId: Int) {
        pendingOpenNotificationIdState.value = notificationId
        awaitingOpenReturnState.value = false
        suppressNotificationFocusUpdates = true
        showOpeningOverlayImmediately = true
    }

    fun markOpenRequestAwaitingReturn() {
        awaitingOpenReturnState.value = true
    }

    fun clearOpenNotificationRequest() {
        pendingOpenNotificationIdState.value = null
        awaitingOpenReturnState.value = false
        suppressNotificationFocusUpdates = false
    }

    fun clearShowOpeningOverlayImmediately() {
        showOpeningOverlayImmediately = false
    }

    fun beginPendingReadFocus(notificationId: Int) {
        pendingReadFocusIdState.value = notificationId
        pendingReadSawLoadingState.value = false
    }

    fun markPendingReadSawLoading() {
        pendingReadSawLoadingState.value = true
    }

    fun clearPendingReadFocus() {
        pendingReadFocusIdState.value = null
        pendingReadSawLoadingState.value = false
    }

    fun beginPendingDeleteFocus(notificationId: Int, fallbackIndex: Int) {
        pendingDeletedNotificationIdState.value = notificationId
        pendingDeleteFallbackIndexState.value = fallbackIndex
        pendingDeleteSawLoadingState.value = false
    }

    fun markPendingDeleteSawLoading() {
        pendingDeleteSawLoadingState.value = true
    }

    fun clearPendingDeleteFocus() {
        pendingDeletedNotificationIdState.value = null
        pendingDeleteFallbackIndexState.value = null
        pendingDeleteSawLoadingState.value = false
    }

    /** Called from a row/placeholder's `onFocusChanged` when it gains focus. */
    fun markContentFocused() {
        notificationContentHasFocus = true
    }

    // ---------------- FocusRequester lookups ----------------

    /** Index 0 aliases [contentFocusRequester] (see class doc); everything else is id-cached. */
    fun rowFocusRequester(index: Int, notificationId: Int): FocusRequester =
        if (index == 0) contentFocusRequester else rowFocusRequesterCache.getOrPut(notificationId) { FocusRequester() }

    fun readFocusRequester(notificationId: Int): FocusRequester =
        readFocusRequesterCache.getOrPut(notificationId) { FocusRequester() }

    fun deleteFocusRequester(notificationId: Int): FocusRequester =
        deleteFocusRequesterCache.getOrPut(notificationId) { FocusRequester() }

    /** Built on demand for [launchTvLazyListKeyFocusRestore]; same index-0 aliasing as [rowFocusRequester]. */
    fun rowFocusRequestersById(notificationIds: List<Int>): Map<Int, FocusRequester> =
        notificationIds.mapIndexed { index, id -> id to rowFocusRequester(index, id) }.toMap()

    /** Drops cache entries for ids no longer in the list so the caches don't grow unboundedly. */
    fun pruneStaleFocusRequesters(notificationIds: List<Int>) {
        val liveIds = notificationIds.toSet()
        rowFocusRequesterCache.keys.retainAll(liveIds)
        readFocusRequesterCache.keys.retainAll(liveIds)
        deleteFocusRequesterCache.keys.retainAll(liveIds)
    }

    fun preferredNotificationFocusRequester(notificationIds: List<Int>): FocusRequester {
        val index =
            focusRestoreState.targetIndex(notificationIds)?.takeIf { it in notificationIds.indices }
                ?: return contentFocusRequester
        return rowFocusRequester(index, notificationIds[index])
    }

    fun notificationListFallbackFocusRequester(
        notificationIds: List<Int>,
        selectedTabFocusRequester: FocusRequester?,
    ): FocusRequester =
        if (shouldRestoreNotificationItemFocus) {
            preferredNotificationFocusRequester(notificationIds)
        } else {
            selectedTabFocusRequester ?: preferredNotificationFocusRequester(notificationIds)
        }

    // ---------------- vertical D-pad neighbors ----------------

    fun previousVerticalFocusRequester(
        index: Int,
        notificationIds: List<Int>,
        selectedTabFocusRequester: FocusRequester?,
    ): FocusRequester? = when {
        index == 0 -> selectedTabFocusRequester
        index > 0 -> rowFocusRequester(index - 1, notificationIds[index - 1])
        else -> null
    }

    fun nextVerticalFocusRequester(index: Int, notificationIds: List<Int>): FocusRequester? =
        if (index < notificationIds.lastIndex) rowFocusRequester(
            index + 1,
            notificationIds[index + 1]
        ) else null

    // ---------------- focus request/scroll orchestration ----------------

    fun requestFocusSafely(focusRequester: FocusRequester): Boolean =
        runCatching { focusRequester.requestFocus() }.getOrDefault(false)

    private fun notificationItemIndex(index: Int): Int = NOTIFICATIONS_CONTENT_ITEM_INDEX + index

    fun requestMainMenuFocus(mainMenuFocusRequester: FocusRequester?): Boolean {
        val requester = mainMenuFocusRequester ?: return false
        notificationContentHasFocus = false
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch { requestFocusUntilTimeout(requester) }
        return true
    }

    fun recordNotificationFocus(index: Int, notificationIds: List<Int>) {
        if (index !in notificationIds.indices) return
        focusRestoreState.onItemFocused(notificationIds[index], index)
    }

    fun handleNotificationFocus(index: Int, notificationIds: List<Int>) {
        if (suppressNotificationFocusUpdates) return
        recordNotificationFocus(index, notificationIds)
    }

    suspend fun requestFocusAfterScroll(
        index: Int,
        notificationIds: List<Int>,
        focusRequester: FocusRequester,
        animateScroll: Boolean = false,
    ) {
        if (index !in notificationIds.indices) return
        recordNotificationFocus(index, notificationIds)
        val itemIndex = notificationItemIndex(index)
        if (animateScroll) {
            listState.animateScrollToItem(itemIndex)
        } else {
            listState.scrollToItem(itemIndex)
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }
        }.first { it }
        requestFocusUntilTimeout(focusRequester)
        if (!animateScroll) {
            listState.scrollToItem(itemIndex)
        }
    }

    suspend fun requestEmptyFocusAfterScroll(animateScroll: Boolean = false) {
        if (animateScroll) {
            listState.animateScrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        } else {
            listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == NOTIFICATIONS_CONTENT_ITEM_INDEX }
        }.first { it }
        requestFocusUntilTimeout(contentFocusRequester)
        if (!animateScroll) {
            listState.scrollToItem(NOTIFICATIONS_CONTENT_ITEM_INDEX)
        }
    }

    private suspend fun requestSelectedTabFocusAfterScroll(
        selectedTabFocusRequester: FocusRequester,
        animateScroll: Boolean = false,
    ): Boolean {
        notificationContentHasFocus = false
        if (animateScroll) {
            listState.animateScrollToItem(NOTIFICATIONS_HEADER_ITEM_INDEX)
        } else {
            listState.scrollToItem(NOTIFICATIONS_HEADER_ITEM_INDEX)
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == NOTIFICATIONS_TABS_ITEM_INDEX }
        }.first { it }
        return requestFocusUntilTimeout(selectedTabFocusRequester)
    }

    fun requestSelectedTabFocus(selectedTabFocusRequester: FocusRequester?): Boolean {
        val requester = selectedTabFocusRequester ?: return false
        notificationContentHasFocus = false
        val isHeaderAtTop = listState.firstVisibleItemIndex == NOTIFICATIONS_HEADER_ITEM_INDEX &&
                listState.firstVisibleItemScrollOffset == 0
        if (
            isHeaderAtTop &&
            listState.layoutInfo.visibleItemsInfo.any { it.index == NOTIFICATIONS_TABS_ITEM_INDEX } &&
            requestFocusSafely(requester)
        ) {
            return true
        }
        focusMoveJob?.cancel()
        focusMoveJob =
            scope.launch { requestSelectedTabFocusAfterScroll(requester, animateScroll = true) }
        return true
    }

    suspend fun requestFocusKeepingCurrentScroll(focusRequester: FocusRequester): Boolean {
        val firstVisibleIndex = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
        val focused = requestFocusSafely(focusRequester)
        // Focusing a partially visible item can trigger the system's bring-into-view scroll;
        // cancel it by restoring the scroll position once the focus change has settled instead
        // of fighting it across several frames, which was visibly janky on TV.
        withFrameNanos { }
        if (
            listState.firstVisibleItemIndex != firstVisibleIndex ||
            listState.firstVisibleItemScrollOffset != firstVisibleOffset
        ) {
            listState.scrollToItem(firstVisibleIndex, firstVisibleOffset)
        }
        return focused
    }

    fun requestFocusAtIndex(
        index: Int,
        notificationIds: List<Int>,
        focusRequester: FocusRequester,
        alignScroll: Boolean = false,
    ) {
        if (index !in notificationIds.indices) return
        focusMoveJob?.cancel()
        recordNotificationFocus(index, notificationIds)
        val itemIndex = notificationItemIndex(index)
        if (!alignScroll && listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }) {
            requestFocusSafely(focusRequester)
        } else {
            focusMoveJob = scope.launch {
                requestFocusAfterScroll(
                    index = index,
                    notificationIds = notificationIds,
                    focusRequester = focusRequester,
                    animateScroll = true,
                )
            }
        }
    }

    fun requestNotificationFocus(index: Int, notificationIds: List<Int>): Boolean {
        if (index !in notificationIds.indices) return false
        handleNotificationFocus(index, notificationIds)
        requestFocusAtIndex(
            index = index,
            notificationIds = notificationIds,
            focusRequester = rowFocusRequester(index, notificationIds[index]),
        )
        return true
    }

    fun requestPreviousNotificationFocus(
        index: Int,
        notificationIds: List<Int>,
        selectedTabFocusRequester: FocusRequester?,
    ): Boolean =
        if (index > 0) {
            requestNotificationFocus(index - 1, notificationIds)
        } else {
            requestSelectedTabFocus(selectedTabFocusRequester)
        }

    fun requestDeleteFocus(index: Int, notificationIds: List<Int>) {
        if (index !in notificationIds.indices) return
        handleNotificationFocus(index, notificationIds)
        requestFocusAtIndex(index, notificationIds, deleteFocusRequester(notificationIds[index]))
    }

    fun requestEmptyOrTopFocus() {
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch { requestEmptyFocusAfterScroll() }
    }

    /** Down/Center/Enter on the tab row asked to focus whatever's currently in the list. */
    fun requestContentFocusFromTabs(notificationIds: List<Int>) {
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch {
            notificationContentHasFocus = true
            if (notificationIds.isEmpty()) {
                if (listState.layoutInfo.visibleItemsInfo.any { it.index == NOTIFICATIONS_CONTENT_ITEM_INDEX }) {
                    requestFocusKeepingCurrentScroll(contentFocusRequester)
                } else {
                    // Restoring a remembered position is automatic, not user-paced, so jump
                    // straight there instead of animating - animateScrollToItem snaps most of
                    // the way for distant targets anyway, which reads as a jerky double-jump.
                    requestEmptyFocusAfterScroll(animateScroll = false)
                }
            } else {
                val targetIndex = focusRestoreState.targetIndex(notificationIds) ?: 0
                handleNotificationFocus(targetIndex, notificationIds)
                val requester = rowFocusRequester(targetIndex, notificationIds[targetIndex])
                if (listState.layoutInfo.visibleItemsInfo.any {
                        it.index == notificationItemIndex(
                            targetIndex
                        )
                    }) {
                    requestFocusKeepingCurrentScroll(requester)
                } else {
                    requestFocusAfterScroll(
                        targetIndex,
                        notificationIds,
                        requester,
                        animateScroll = false
                    )
                }
            }
        }
    }

    /** Resumes the "open notification -> navigate -> come back" flow once we're active again. */
    fun beginRestoreAfterNotificationOpen(notificationIds: List<Int>) {
        val request = openNotificationRequest ?: return
        val targetIndex = notificationIds.indexOf(request.notificationId)
            .takeIf { it >= 0 }
            ?: focusRestoreState.targetIndex(notificationIds)
        if (targetIndex == null || targetIndex !in notificationIds.indices) {
            clearOpenNotificationRequest()
            return
        }
        suppressNotificationFocusUpdates = true
        notificationContentHasFocus = true
        recordNotificationFocus(targetIndex, notificationIds)
        val fallback = rowFocusRequester(targetIndex, notificationIds[targetIndex])
        restoreFocusJob = launchTvLazyListKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = notificationIds,
            listState = listState,
            itemFocusRequesters = rowFocusRequestersById(notificationIds),
            fallbackFocusRequester = fallback,
            fallbackIndex = targetIndex,
            lazyIndexOffset = NOTIFICATIONS_CONTENT_ITEM_INDEX,
            onRestoreFinished = { clearOpenNotificationRequest() },
        )
    }
}

@Composable
internal fun rememberNotificationsTabState(): NotificationsTabState {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>("account_notifications")
    val pendingOpenNotificationIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val awaitingOpenReturnState = rememberSaveable { mutableStateOf(false) }
    val pendingReadFocusIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingReadSawLoadingState = rememberSaveable { mutableStateOf(false) }
    val pendingDeletedNotificationIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingDeleteFallbackIndexState = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingDeleteSawLoadingState = rememberSaveable { mutableStateOf(false) }
    return remember {
        NotificationsTabState(
            scope = scope,
            listState = listState,
            focusRestoreState = focusRestoreState,
            pendingOpenNotificationIdState = pendingOpenNotificationIdState,
            awaitingOpenReturnState = awaitingOpenReturnState,
            pendingReadFocusIdState = pendingReadFocusIdState,
            pendingReadSawLoadingState = pendingReadSawLoadingState,
            pendingDeletedNotificationIdState = pendingDeletedNotificationIdState,
            pendingDeleteFallbackIndexState = pendingDeleteFallbackIndexState,
            pendingDeleteSawLoadingState = pendingDeleteSawLoadingState,
        )
    }
}

/** All reactive side effects for [NotificationsTabState], kept out of the screen composable. */
@Composable
internal fun NotificationsTabEffects(
    notificationsTabState: NotificationsTabState,
    notificationIds: List<Int>,
    notificationReadStates: List<Boolean>,
    isActiveDestination: Boolean,
    isNotificationsLoading: Boolean,
    isNotificationOpening: Boolean,
    showOpeningOverlay: Boolean,
    hubError: AccountUiError?,
) {
    // 1. Resume the open-notification restore flow once we're active again after navigating away.
    LaunchedEffect(
        isActiveDestination,
        notificationsTabState.openNotificationRequest,
        notificationIds,
    ) {
        val request = notificationsTabState.openNotificationRequest ?: return@LaunchedEffect
        if (!isActiveDestination) {
            notificationsTabState.markOpenRequestAwaitingReturn()
            return@LaunchedEffect
        }
        if (!request.awaitingReturn) return@LaunchedEffect
        notificationsTabState.beginRestoreAfterNotificationOpen(notificationIds)
    }

    // 2. Remember whether a loading flash was seen while a read/delete focus move is pending -
    // effects 5 and 6 use this to know when to give up waiting on a mutation that never lands.
    LaunchedEffect(
        isNotificationsLoading,
        notificationsTabState.pendingReadFocus,
        notificationsTabState.pendingDeleteFocus,
    ) {
        if (isNotificationsLoading) {
            if (notificationsTabState.pendingReadFocus != null) notificationsTabState.markPendingReadSawLoading()
            if (notificationsTabState.pendingDeleteFocus != null) notificationsTabState.markPendingDeleteSawLoading()
        }
    }

    // 3. Keep the full-screen opening overlay from ever losing focus while it's shown.
    LaunchedEffect(showOpeningOverlay) {
        if (showOpeningOverlay) {
            requestFocusUntilTimeout(notificationsTabState.openingOverlayFocusRequester)
        }
    }

    // 4. Clear the overlay/open-request if opening failed or we left the screen entirely.
    LaunchedEffect(isNotificationOpening, hubError, isActiveDestination) {
        if (
            notificationsTabState.showOpeningOverlayImmediately &&
            (!isActiveDestination || (!isNotificationOpening && hubError != null))
        ) {
            notificationsTabState.clearShowOpeningOverlayImmediately()
        }
        if (
            notificationsTabState.openNotificationRequest != null &&
            !isNotificationOpening &&
            hubError != null
        ) {
            notificationsTabState.clearOpenNotificationRequest()
        }
    }

    // 5. Once a marked-read notification's `viewed` flag flips, move focus onto its Delete button.
    LaunchedEffect(
        notificationIds,
        notificationReadStates,
        notificationsTabState.pendingReadFocus,
        isNotificationsLoading,
    ) {
        val pending = notificationsTabState.pendingReadFocus ?: return@LaunchedEffect
        val targetIndex = notificationIds.indexOf(pending.notificationId)
        when {
            targetIndex < 0 -> {
                if (pending.sawLoading && !isNotificationsLoading) {
                    notificationsTabState.clearPendingReadFocus()
                }
            }

            notificationReadStates[targetIndex] -> {
                val deleteRequester =
                    notificationsTabState.deleteFocusRequester(pending.notificationId)
                notificationsTabState.handleNotificationFocus(targetIndex, notificationIds)
                notificationsTabState.requestFocusAfterScroll(
                    targetIndex,
                    notificationIds,
                    deleteRequester
                )
                notificationsTabState.clearPendingReadFocus()
            }

            pending.sawLoading && !isNotificationsLoading -> {
                notificationsTabState.clearPendingReadFocus()
            }
        }
    }

    // 6. Once a deleted notification actually leaves the list, restore focus near where it was.
    LaunchedEffect(
        notificationIds,
        notificationsTabState.pendingDeleteFocus,
        isNotificationsLoading,
    ) {
        val pending = notificationsTabState.pendingDeleteFocus ?: return@LaunchedEffect
        if (notificationIds.contains(pending.notificationId)) {
            if (pending.sawLoading && !isNotificationsLoading) {
                notificationsTabState.clearPendingDeleteFocus()
            }
            return@LaunchedEffect
        }

        if (notificationIds.isEmpty()) {
            notificationsTabState.requestEmptyOrTopFocus()
        } else {
            val restoredIndex = pending.fallbackIndex.coerceIn(0, notificationIds.lastIndex)
            notificationsTabState.handleNotificationFocus(restoredIndex, notificationIds)
            notificationsTabState.requestFocusAfterScroll(
                index = restoredIndex,
                notificationIds = notificationIds,
                focusRequester = notificationsTabState.rowFocusRequester(
                    restoredIndex,
                    notificationIds[restoredIndex]
                ),
            )
        }
        notificationsTabState.clearPendingDeleteFocus()
    }

    // 7. Cache hygiene: drop FocusRequesters for notifications that are no longer in the list.
    LaunchedEffect(notificationIds) {
        notificationsTabState.pruneStaleFocusRequesters(notificationIds)
    }
}

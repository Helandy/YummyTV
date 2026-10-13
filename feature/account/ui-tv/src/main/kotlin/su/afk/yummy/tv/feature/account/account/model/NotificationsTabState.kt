package su.afk.yummy.tv.feature.account.account.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

/**
 * Item order the NotificationsTab LazyColumn is built in: header=0, tabs+badges+actions=1,
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
 * in the paired NotificationsTabEffects composable. Pending operations live in [pendingFocus],
 * FocusRequesters in [focusRequesters].
 *
 * Never stores the notification list itself - callers pass the current `notificationIds` into
 * every method that needs list bounds or id lookups, so this class can't act on stale data.
 */
@Stable
internal class NotificationsTabState internal constructor(
    private val scope: CoroutineScope,
    val listState: LazyListState,
    val focusRestoreState: TvLazyFocusRestoreState<Int>,
    val pendingFocus: NotificationsPendingFocus,
) {
    val focusRequesters = NotificationsFocusRequesters()

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
        get() = pendingFocus.openNotificationRequest

    val shouldRestoreNotificationItemFocus: Boolean
        get() = notificationContentHasFocus ||
                openNotificationRequest != null ||
                suppressNotificationFocusUpdates

    // ---------------- open-notification flow ----------------

    /** Call when the user activates an openable notification row. */
    fun beginOpeningNotification(notificationId: Int) {
        pendingFocus.beginOpenRequest(notificationId)
        suppressNotificationFocusUpdates = true
        showOpeningOverlayImmediately = true
    }

    fun clearOpenNotificationRequest() {
        pendingFocus.clearOpenRequest()
        suppressNotificationFocusUpdates = false
    }

    fun clearShowOpeningOverlayImmediately() {
        showOpeningOverlayImmediately = false
    }

    /** Called from a row/placeholder's `onFocusChanged` when it gains focus. */
    fun markContentFocused() {
        notificationContentHasFocus = true
    }

    // ---------------- FocusRequester choice ----------------

    fun preferredNotificationFocusRequester(notificationIds: List<Int>): FocusRequester {
        val index =
            focusRestoreState.targetIndex(notificationIds)?.takeIf { it in notificationIds.indices }
                ?: return focusRequesters.contentFocusRequester
        return focusRequesters.rowFocusRequester(index, notificationIds[index])
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
        index > 0 -> focusRequesters.rowFocusRequester(index - 1, notificationIds[index - 1])
        else -> null
    }

    fun nextVerticalFocusRequester(index: Int, notificationIds: List<Int>): FocusRequester? =
        if (index < notificationIds.lastIndex) focusRequesters.rowFocusRequester(
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
        requestFocusUntilTimeout(focusRequesters.contentFocusRequester)
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
            focusRequester = focusRequesters.rowFocusRequester(index, notificationIds[index]),
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
        requestFocusAtIndex(index, notificationIds, focusRequesters.deleteFocusRequester(notificationIds[index]))
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
                    requestFocusKeepingCurrentScroll(focusRequesters.contentFocusRequester)
                } else {
                    // Restoring a remembered position is automatic, not user-paced, so jump
                    // straight there instead of animating - animateScrollToItem snaps most of
                    // the way for distant targets anyway, which reads as a jerky double-jump.
                    requestEmptyFocusAfterScroll(animateScroll = false)
                }
            } else {
                val targetIndex = focusRestoreState.targetIndex(notificationIds) ?: 0
                handleNotificationFocus(targetIndex, notificationIds)
                val requester = focusRequesters.rowFocusRequester(targetIndex, notificationIds[targetIndex])
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
        val fallback = focusRequesters.rowFocusRequester(targetIndex, notificationIds[targetIndex])
        restoreFocusJob = launchTvLazyListKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = notificationIds,
            listState = listState,
            itemFocusRequesters = focusRequesters.rowFocusRequestersById(notificationIds),
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
    val pendingFocus = rememberNotificationsPendingFocus()
    return remember {
        NotificationsTabState(
            scope = scope,
            listState = listState,
            focusRestoreState = focusRestoreState,
            pendingFocus = pendingFocus,
        )
    }
}

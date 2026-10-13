package su.afk.yummy.tv.feature.account.account.model

import androidx.compose.ui.focus.FocusRequester

/**
 * FocusRequester'ы вкладки уведомлений: постоянные (кнопки над списком, оверлей открытия) и
 * кэшированные по id уведомления для строк и их кнопок «Прочитано» / «Удалить».
 */
internal class NotificationsFocusRequesters {
    /**
     * Triple role, intentional: passed to AccountTabs as its `contentFocusRequester` (Down/
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

    /** Index 0 aliases [contentFocusRequester] (see its doc); everything else is id-cached. */
    fun rowFocusRequester(index: Int, notificationId: Int): FocusRequester =
        if (index == 0) contentFocusRequester else rowFocusRequesterCache.getOrPut(notificationId) { FocusRequester() }

    fun readFocusRequester(notificationId: Int): FocusRequester =
        readFocusRequesterCache.getOrPut(notificationId) { FocusRequester() }

    fun deleteFocusRequester(notificationId: Int): FocusRequester =
        deleteFocusRequesterCache.getOrPut(notificationId) { FocusRequester() }

    /** Built on demand for list key focus restore; same index-0 aliasing as [rowFocusRequester]. */
    fun rowFocusRequestersById(notificationIds: List<Int>): Map<Int, FocusRequester> =
        notificationIds.mapIndexed { index, id -> id to rowFocusRequester(index, id) }.toMap()

    /** Drops cache entries for ids no longer in the list so the caches don't grow unboundedly. */
    fun pruneStaleFocusRequesters(notificationIds: List<Int>) {
        val liveIds = notificationIds.toSet()
        rowFocusRequesterCache.keys.retainAll(liveIds)
        readFocusRequesterCache.keys.retainAll(liveIds)
        deleteFocusRequesterCache.keys.retainAll(liveIds)
    }
}

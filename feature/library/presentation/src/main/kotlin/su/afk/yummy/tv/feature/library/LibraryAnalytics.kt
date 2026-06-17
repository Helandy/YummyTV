package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import javax.inject.Inject

internal class LibraryAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл экран библиотеки.
     */
    fun eventScreenOpened() {
        tracker.track(EVENT_SCREEN_OPENED)
    }

    /**
     * Пользователь выбрал аниме из библиотеки.
     *
     * Параметры: anime_id, tab.
     */
    fun eventAnimeSelected(animeId: Int, tab: LibraryTab) {
        tracker.track(
            EVENT_ANIME_SELECTED,
            analyticsParamsOf(
                PARAM_ANIME_ID to animeId,
                PARAM_TAB to tab.analyticsValue(),
            ),
        )
    }

    /**
     * Пользователь продолжил просмотр из библиотеки.
     *
     * Параметры: anime_id, video_id.
     */
    fun eventContinueWatchingSelected(entry: HomeContinueWatchingItem) {
        tracker.track(
            EVENT_CONTINUE_WATCHING_SELECTED,
            analyticsParamsOf(
                PARAM_ANIME_ID to entry.animeId,
                PARAM_VIDEO_ID to entry.videoId,
            ),
        )
    }

    /**
     * Пользователь открыл детали из продолжения просмотра.
     *
     * Параметры: anime_id, video_id.
     */
    fun eventContinueWatchingDetailsSelected(entry: HomeContinueWatchingItem) {
        tracker.track(
            EVENT_CONTINUE_WATCHING_DETAILS_SELECTED,
            analyticsParamsOf(
                PARAM_ANIME_ID to entry.animeId,
                PARAM_VIDEO_ID to entry.videoId,
            ),
        )
    }

    /**
     * Пользователь выбрал вкладку библиотеки.
     *
     * Параметры: tab.
     */
    fun eventTabSelected(tab: LibraryTab) {
        tracker.track(EVENT_TAB_SELECTED, analyticsParamsOf(PARAM_TAB to tab.analyticsValue()))
    }

    /**
     * Пользователь повторил загрузку удаленных списков библиотеки.
     */
    fun eventRetry() {
        tracker.track(EVENT_RETRY)
    }

    /**
     * Пользователь удалил прогресс просмотра.
     *
     * Параметры: anime_id.
     */
    fun eventRemoveWatchProgress(animeId: Int) {
        tracker.track(EVENT_REMOVE_WATCH_PROGRESS, analyticsParamsOf(PARAM_ANIME_ID to animeId))
    }

    /**
     * Пользователь удалил запись из библиотеки.
     *
     * Параметры: anime_id, tab, target, remote, list.
     */
    fun eventRemoveEntry(
        animeId: Int,
        tab: LibraryTab,
        target: LibraryRemoveTarget,
        remote: Boolean,
        list: UserAnimeList?,
    ) {
        tracker.track(
            EVENT_REMOVE_ENTRY,
            analyticsParamsOf(
                PARAM_ANIME_ID to animeId,
                PARAM_TAB to tab.analyticsValue(),
                PARAM_TARGET to target.analyticsValue(),
                PARAM_REMOTE to remote,
                PARAM_LIST to list?.name?.lowercase(),
            ),
        )
    }

    /**
     * Удаленные списки библиотеки не загрузились.
     */
    fun eventLoadError(error: Throwable) {
        tracker.reportError(
            groupIdentifier = EVENT_LOAD_ERROR,
            message = "$LOAD_ERROR_MESSAGE_PREFIX: ${error.analyticsType()}",
            throwable = error,
        )
    }

    /**
     * Сетевое удаление записи из библиотеки не выполнилось.
     */
    fun eventRemoveError(target: LibraryRemoveTarget, error: Throwable) {
        tracker.reportError(
            groupIdentifier = EVENT_REMOVE_ERROR,
            message = "$REMOVE_ERROR_MESSAGE_PREFIX " +
                    "(target=${target.analyticsValue()}): ${error.analyticsType()}",
            throwable = error,
        )
    }

    private fun LibraryTab.analyticsValue(): String = name.lowercase()

    private fun LibraryRemoveTarget.analyticsValue(): String = name.lowercase()

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_LIST = "list"
        private const val PARAM_REMOTE = "remote"
        private const val PARAM_TAB = "tab"
        private const val PARAM_TARGET = "target"
        private const val PARAM_VIDEO_ID = "video_id"
        private const val LOAD_ERROR_MESSAGE_PREFIX = "Library load failed"
        private const val REMOVE_ERROR_MESSAGE_PREFIX = "Library remove failed"

        const val EVENT_SCREEN_OPENED = "library_screen"
        const val EVENT_ANIME_SELECTED = "library_anime_selected"
        const val EVENT_CONTINUE_WATCHING_SELECTED = "library_continue_watching_selected"
        const val EVENT_CONTINUE_WATCHING_DETAILS_SELECTED =
            "library_continue_watching_details_selected"
        const val EVENT_TAB_SELECTED = "library_tab_selected"
        const val EVENT_RETRY = "library_retry"
        const val EVENT_REMOVE_ENTRY = "library_remove_entry"
        const val EVENT_REMOVE_WATCH_PROGRESS = "library_remove_watch_progress"
        const val EVENT_LOAD_ERROR = "library_load_error"
        const val EVENT_REMOVE_ERROR = "library_remove_error"
    }
}

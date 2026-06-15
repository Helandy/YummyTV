package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import javax.inject.Inject

internal class LibraryAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь выбрал локальное аниме из библиотеки.
     *
     * Параметры: anime_id, source, tab.
     */
    fun eventLocalAnimeSelected(animeId: Int, tab: LibraryTab) {
        eventAnimeSelected(animeId, SOURCE_LOCAL, tab)
    }

    /**
     * Пользователь выбрал удаленное аниме из библиотеки.
     *
     * Параметры: anime_id, source, tab.
     */
    fun eventRemoteAnimeSelected(animeId: Int, tab: LibraryTab) {
        eventAnimeSelected(animeId, SOURCE_REMOTE, tab)
    }

    private fun eventAnimeSelected(animeId: Int, source: String, tab: LibraryTab) {
        tracker.track(
            EVENT_ANIME_SELECTED,
            analyticsParamsOf(
                PARAM_ANIME_ID to animeId,
                PARAM_SOURCE to source,
                PARAM_TAB to tab.analyticsValue(),
            ),
        )
    }

    /**
     * Пользователь продолжил просмотр из библиотеки.
     *
     * Параметры: anime_id, video_id.
     */
    fun eventContinueWatchingSelected(entry: WatchProgressEntry) {
        tracker.track(
            EVENT_CONTINUE_WATCHING_SELECTED,
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
     * Пользователь удалил локальную запись из библиотеки.
     *
     * Параметры: anime_id.
     */
    fun eventRemoveLibraryEntry(animeId: Int) {
        tracker.track(EVENT_REMOVE_LIBRARY_ENTRY, analyticsParamsOf(PARAM_ANIME_ID to animeId))
    }

    /**
     * Пользователь удалил аниме из избранного.
     *
     * Параметры: anime_id.
     */
    fun eventRemoveFavoriteEntry(animeId: Int) {
        tracker.track(EVENT_REMOVE_FAVORITE_ENTRY, analyticsParamsOf(PARAM_ANIME_ID to animeId))
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
     * Пользователь удалил запись из удаленного списка Yani.
     *
     * Параметры: anime_id, tab, favorite, list.
     */
    fun eventRemoveRemoteEntry(
        animeId: Int,
        tab: LibraryTab,
        favorite: Boolean,
        list: UserAnimeList?,
    ) {
        tracker.track(
            EVENT_REMOVE_REMOTE_ENTRY,
            analyticsParamsOf(
                PARAM_ANIME_ID to animeId,
                PARAM_TAB to tab.analyticsValue(),
                PARAM_FAVORITE to favorite,
                PARAM_LIST to list?.name?.lowercase(),
            ),
        )
    }

    private fun LibraryTab.analyticsValue(): String = name.lowercase()

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_FAVORITE = "favorite"
        private const val PARAM_LIST = "list"
        private const val PARAM_SOURCE = "source"
        private const val PARAM_TAB = "tab"
        private const val PARAM_VIDEO_ID = "video_id"
        private const val SOURCE_LOCAL = "local"
        private const val SOURCE_REMOTE = "remote"

        const val EVENT_ANIME_SELECTED = "library_anime_selected"
        const val EVENT_CONTINUE_WATCHING_SELECTED = "library_continue_watching_selected"
        const val EVENT_TAB_SELECTED = "library_tab_selected"
        const val EVENT_RETRY = "library_retry"
        const val EVENT_REMOVE_LIBRARY_ENTRY = "library_remove_library_entry"
        const val EVENT_REMOVE_FAVORITE_ENTRY = "library_remove_favorite_entry"
        const val EVENT_REMOVE_WATCH_PROGRESS = "library_remove_watch_progress"
        const val EVENT_REMOVE_REMOTE_ENTRY = "library_remove_remote_entry"
    }
}

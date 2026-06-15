package su.afk.yummy.tv.feature.home

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import javax.inject.Inject

internal class HomeAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь выбрал аниме на главном экране.
     *
     * Параметры: anime_id.
     */
    fun eventAnimeSelected(animeId: Int) {
        tracker.track(EVENT_ANIME_SELECTED, analyticsParamsOf(PARAM_ANIME_ID to animeId))
    }

    /**
     * Пользователь выбрал коллекцию на главном экране.
     *
     * Параметры: collection_id.
     */
    fun eventCollectionSelected(collectionId: Int) {
        tracker.track(
            EVENT_COLLECTION_SELECTED,
            analyticsParamsOf(PARAM_COLLECTION_ID to collectionId)
        )
    }

    /**
     * Пользователь продолжил просмотр с главного экрана.
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
     * Пользователь повторил загрузку главного экрана.
     */
    fun eventRetry() {
        tracker.track(EVENT_RETRY)
    }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_COLLECTION_ID = "collection_id"
        private const val PARAM_VIDEO_ID = "video_id"

        const val EVENT_ANIME_SELECTED = "home_anime_selected"
        const val EVENT_COLLECTION_SELECTED = "home_collection_selected"
        const val EVENT_CONTINUE_WATCHING_SELECTED = "home_continue_watching_selected"
        const val EVENT_RETRY = "home_retry"
    }
}

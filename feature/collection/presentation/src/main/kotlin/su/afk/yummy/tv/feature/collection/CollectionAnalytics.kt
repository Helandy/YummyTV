package su.afk.yummy.tv.feature.collection

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class CollectionAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл экран коллекции.
     *
     * Параметры: collection_id.
     */
    fun eventScreenOpened(collectionId: Int) {
        tracker.track(
            EVENT_SCREEN_OPENED,
            analyticsParamsOf(PARAM_COLLECTION_ID to collectionId),
        )
    }

    /**
     * Пользователь повторил загрузку коллекции.
     *
     * Параметры: collection_id.
     */
    fun eventRetry(collectionId: Int) {
        tracker.track(EVENT_RETRY, analyticsParamsOf(PARAM_COLLECTION_ID to collectionId))
    }

    /**
     * Пользователь выбрал аниме из коллекции.
     *
     * Параметры: collection_id, anime_id.
     */
    fun eventAnimeSelected(collectionId: Int, animeId: Int) {
        tracker.track(
            EVENT_ANIME_SELECTED,
            analyticsParamsOf(
                PARAM_COLLECTION_ID to collectionId,
                PARAM_ANIME_ID to animeId,
            ),
        )
    }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_COLLECTION_ID = "collection_id"

        const val EVENT_SCREEN_OPENED = "collection_screen"
        const val EVENT_RETRY = "collection_retry"
        const val EVENT_ANIME_SELECTED = "collection_anime_selected"
    }
}

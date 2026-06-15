package su.afk.yummy.tv.feature.top

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import javax.inject.Inject

internal class TopAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь выбрал тип топа.
     *
     * Параметры: type.
     */
    fun eventTypeSelected(type: AnimeTopType) {
        tracker.track(EVENT_TYPE_SELECTED, analyticsParamsOf(PARAM_TYPE to type.name.lowercase()))
    }

    /**
     * Пользователь выбрал аниме из топа.
     *
     * Параметры: anime_id.
     */
    fun eventAnimeSelected(animeId: Int) {
        tracker.track(EVENT_ANIME_SELECTED, analyticsParamsOf(PARAM_ANIME_ID to animeId))
    }

    /**
     * Пользователь повторил загрузку топа.
     */
    fun eventRetry() {
        tracker.track(EVENT_RETRY)
    }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_TYPE = "type"

        const val EVENT_TYPE_SELECTED = "top_type_selected"
        const val EVENT_ANIME_SELECTED = "top_anime_selected"
        const val EVENT_RETRY = "top_retry"
    }
}

package su.afk.yummy.tv.feature.search

import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class SearchAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь выбрал аниме из результатов поиска.
     *
     * Параметры: anime_id.
     */
    fun eventAnimeSelected(animeId: Int) {
        tracker.track(EVENT_ANIME_SELECTED, analyticsParamsOf(PARAM_ANIME_ID to animeId))
    }

    /**
     * Пользователь открыл панель фильтров поиска.
     */
    fun eventFiltersOpened() {
        tracker.track(EVENT_FILTERS_OPENED)
    }

    /**
     * Пользователь закрыл панель фильтров поиска без применения.
     */
    fun eventFiltersClosed() {
        tracker.track(EVENT_FILTERS_CLOSED)
    }

    /**
     * Пользователь явно отправил поисковый запрос из внешнего ввода.
     *
     * Параметры: screen, has_query, filter_count, source.
     */
    fun eventExternalSearchSubmitted() {
        eventSearchSubmitted(
            hasQuery = true,
            filterCount = 0,
            source = SOURCE_EXTERNAL,
        )
    }

    /**
     * Пользователь явно отправил поисковый запрос из экрана поиска.
     *
     * Параметры: screen, has_query, filter_count, source.
     */
    fun eventManualSearchSubmitted(hasQuery: Boolean, filterCount: Int) {
        eventSearchSubmitted(
            hasQuery = hasQuery,
            filterCount = filterCount,
            source = SOURCE_MANUAL,
        )
    }

    private fun eventSearchSubmitted(hasQuery: Boolean, filterCount: Int, source: String) {
        tracker.track(
            EVENT_SEARCH_SUBMIT,
            analyticsParamsOf(
                AnalyticsEvents.PARAM_SCREEN to SCREEN_SEARCH,
                PARAM_HAS_QUERY to hasQuery,
                PARAM_FILTER_COUNT to filterCount,
                PARAM_SOURCE to source,
            ),
        )
    }

    /**
     * Пользователь применил фильтры поиска.
     *
     * Параметры: screen, action, has_query, filter_count.
     */
    fun eventFiltersApplied(hasQuery: Boolean, filterCount: Int) {
        eventFiltersApply(ACTION_APPLY, hasQuery, filterCount)
    }

    /**
     * Пользователь сбросил фильтры поиска.
     *
     * Параметры: screen, action, has_query, filter_count.
     */
    fun eventFiltersReset(hasQuery: Boolean, filterCount: Int) {
        eventFiltersApply(ACTION_RESET, hasQuery, filterCount)
    }

    private fun eventFiltersApply(action: String, hasQuery: Boolean, filterCount: Int) {
        tracker.track(
            EVENT_SEARCH_FILTERS_APPLY,
            analyticsParamsOf(
                AnalyticsEvents.PARAM_SCREEN to SCREEN_SEARCH,
                AnalyticsEvents.PARAM_ACTION to action,
                PARAM_HAS_QUERY to hasQuery,
                PARAM_FILTER_COUNT to filterCount,
            ),
        )
    }

    internal companion object {
        private const val ACTION_APPLY = "apply"
        private const val ACTION_RESET = "reset"
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_FILTER_COUNT = "filter_count"
        private const val PARAM_HAS_QUERY = "has_query"
        private const val PARAM_SOURCE = "source"
        private const val SCREEN_SEARCH = "search"
        private const val SOURCE_EXTERNAL = "external"
        private const val SOURCE_MANUAL = "manual"

        const val EVENT_ANIME_SELECTED = "search_anime_selected"
        const val EVENT_FILTERS_OPENED = "search_filters_opened"
        const val EVENT_FILTERS_CLOSED = "search_filters_closed"
        const val EVENT_SEARCH_SUBMIT = "search_submit"
        const val EVENT_SEARCH_FILTERS_APPLY = "search_filters_apply"
    }
}

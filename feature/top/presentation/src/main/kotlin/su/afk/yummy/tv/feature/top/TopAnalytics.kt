package su.afk.yummy.tv.feature.top

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import javax.inject.Inject

internal class TopAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл экран топа.
     */
    fun eventScreenOpened() {
        tracker.track(EVENT_SCREEN_OPENED)
    }

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

    /**
     * Экран топа не загрузился.
     */
    fun eventLoadError(type: AnimeTopType, error: Throwable) {
        tracker.reportError(
            groupIdentifier = EVENT_LOAD_ERROR,
            message = "$ERROR_MESSAGE_PREFIX (${type.name.lowercase()}): ${error.analyticsType()}",
            throwable = error,
        )
    }

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_TYPE = "type"
        private const val ERROR_MESSAGE_PREFIX = "Top load failed"

        const val EVENT_SCREEN_OPENED = "top_screen"
        const val EVENT_TYPE_SELECTED = "top_type_selected"
        const val EVENT_ANIME_SELECTED = "top_anime_selected"
        const val EVENT_RETRY = "top_retry"
        const val EVENT_LOAD_ERROR = "top_load_error"
    }
}

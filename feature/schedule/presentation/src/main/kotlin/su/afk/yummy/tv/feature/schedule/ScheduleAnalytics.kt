package su.afk.yummy.tv.feature.schedule

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import su.afk.yummy.tv.core.analytics.utils.analyticsType
import javax.inject.Inject

internal class ScheduleAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл экран расписания.
     */
    fun eventScreenOpened() {
        tracker.track(EVENT_SCREEN_OPENED)
    }

    /**
     * Пользователь выбрал релиз из расписания и перешел к аниме.
     *
     * Параметры: anime_id.
     */
    fun eventAnimeSelected(animeId: Int) {
        tracker.track(EVENT_ANIME_SELECTED, analyticsParamsOf(PARAM_ANIME_ID to animeId))
    }

    /**
     * Пользователь выбрал дату в расписании.
     *
     * Параметры: epoch_day.
     */
    fun eventDateSelected(epochDay: Long) {
        tracker.track(EVENT_DATE_SELECTED, analyticsParamsOf(PARAM_EPOCH_DAY to epochDay))
    }

    /**
     * Пользователь переключил фильтр «В библиотеке».
     *
     * Параметры: enabled.
     */
    fun eventLibraryFilterToggled(enabled: Boolean) {
        tracker.track(EVENT_LIBRARY_FILTER_TOGGLED, analyticsParamsOf(PARAM_ENABLED to enabled))
    }

    /**
     * Пользователь повторил загрузку расписания.
     */
    fun eventRetry() {
        tracker.track(EVENT_RETRY)
    }

    /**
     * Ошибка загрузки расписания.
     */
    fun eventLoadError(throwable: Throwable) {
        tracker.reportError(
            groupIdentifier = EVENT_LOAD_ERROR,
            message = "ScheduleViewModel: ${throwable.analyticsType()}",
            throwable = throwable,
        )
    }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_EPOCH_DAY = "epoch_day"
        private const val PARAM_ENABLED = "enabled"

        const val EVENT_SCREEN_OPENED = "schedule_screen"
        const val EVENT_ANIME_SELECTED = "schedule_anime_selected"
        const val EVENT_DATE_SELECTED = "schedule_date_selected"
        const val EVENT_LIBRARY_FILTER_TOGGLED = "schedule_library_filter_toggled"
        const val EVENT_RETRY = "schedule_retry"
        const val EVENT_LOAD_ERROR = "schedule_load_error"
    }
}

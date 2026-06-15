package su.afk.yummy.tv.feature.schedule

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class ScheduleAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
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
     * Пользователь повторил загрузку расписания.
     */
    fun eventRetry() {
        tracker.track(EVENT_RETRY)
    }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_EPOCH_DAY = "epoch_day"

        const val EVENT_ANIME_SELECTED = "schedule_anime_selected"
        const val EVENT_DATE_SELECTED = "schedule_date_selected"
        const val EVENT_RETRY = "schedule_retry"
    }
}

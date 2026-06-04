package su.afk.yummy.tv.feature.schedule.model

import java.time.LocalDate

data class ScheduleDayUi(
    val date: LocalDate,
    val weekdayLabel: String,
    val items: List<ScheduleReleaseUi>,
)

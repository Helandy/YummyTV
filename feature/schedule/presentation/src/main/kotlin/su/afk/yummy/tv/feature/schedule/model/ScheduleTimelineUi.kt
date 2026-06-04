package su.afk.yummy.tv.feature.schedule.model

import java.time.ZoneId
import java.time.ZonedDateTime

data class ScheduleTimelineUi(
    val zone: ZoneId = ZoneId.systemDefault(),
    val now: ZonedDateTime = ZonedDateTime.now(zone),
    val dayGroups: List<ScheduleDayUi> = emptyList(),
    val selectedEpochDay: Long? = null,
    val focusedReleaseKey: String? = null,
    val focusedReleaseEpochDay: Long? = null,
) {
    val selectedGroup: ScheduleDayUi?
        get() = dayGroups.firstOrNull { it.date.toEpochDay() == selectedEpochDay }
            ?: dayGroups.firstOrNull()

    val availableEpochDays: List<Long>
        get() = dayGroups.map { it.date.toEpochDay() }
}

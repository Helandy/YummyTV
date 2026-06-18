package su.afk.yummy.tv.feature.schedule.utils

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi
import su.afk.yummy.tv.feature.schedule.model.ScheduleReleaseUi
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

internal fun List<AnimeScheduleDay>.toTimelineUi(
    zone: ZoneId = ZoneId.systemDefault(),
    now: ZonedDateTime = ZonedDateTime.now(zone),
    selectedEpochDay: Long? = null,
): ScheduleTimelineUi {
    val today = now.toLocalDate()
    val dayGroups = toUiDayGroups(zone = zone, today = today)
    val availableEpochDays = dayGroups.map { it.date.toEpochDay() }
    val nextSelectedEpochDay = selectedEpochDay(
        availableEpochDays = availableEpochDays,
        todayEpochDay = today.toEpochDay(),
        selectedEpochDay = selectedEpochDay,
    )

    return ScheduleTimelineUi(
        zone = zone,
        now = now,
        dayGroups = dayGroups,
        selectedEpochDay = nextSelectedEpochDay,
    )
}

internal fun ScheduleTimelineUi.withSelectedDay(epochDay: Long): ScheduleTimelineUi =
    copy(selectedEpochDay = epochDay.takeIf { it in availableEpochDays } ?: selectedEpochDay)

private fun selectedEpochDay(
    availableEpochDays: List<Long>,
    todayEpochDay: Long,
    selectedEpochDay: Long?,
): Long? =
    selectedEpochDay?.takeIf { it in availableEpochDays }
        ?: availableEpochDays.firstOrNull { it == todayEpochDay }
        ?: availableEpochDays.firstOrNull { it > todayEpochDay }
        ?: availableEpochDays.firstOrNull()

private fun List<AnimeScheduleDay>.toUiDayGroups(
    zone: ZoneId,
    today: LocalDate,
): List<ScheduleDayUi> =
    flatMap { it.items }
        .flatMap { item ->
            buildList {
                item.previousDateEpochSeconds?.let { previousDate ->
                    add(
                        ScheduleReleaseUi(
                            item = item,
                            epochSeconds = previousDate,
                            episode = item.airedEpisodes ?: 0,
                            aired = true,
                        )
                    )
                }
                item.nextDateEpochSeconds?.let { nextDate ->
                    add(
                        ScheduleReleaseUi(
                            item = item,
                            epochSeconds = nextDate,
                            episode = (item.airedEpisodes ?: 0) + 1,
                            aired = false,
                        )
                    )
                }
            }
        }
        .filter { release ->
            Instant.ofEpochSecond(release.epochSeconds).atZone(zone).toLocalDate() >= today
        }
        .distinctBy { "${it.item.animeId}:${it.epochSeconds}:${it.aired}" }
        .sortedBy { it.epochSeconds }
        .groupBy { release ->
            Instant.ofEpochSecond(release.epochSeconds).atZone(zone).toLocalDate()
        }
        .toSortedMap()
        .map { (date, items) ->
            ScheduleDayUi(
                date = date,
                weekdayLabel = date.dayOfWeek
                    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
                    .trimEnd('.')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                items = items.sortedBy { it.epochSeconds },
            )
        }

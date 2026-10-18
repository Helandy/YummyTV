package su.afk.yummy.tv.feature.account.common.mapper

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.account.model.UserWatchHistoryDay
import su.afk.yummy.tv.feature.account.common.model.WatchHistoryCell
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

const val WATCH_HISTORY_GRID_COLUMNS = 7
const val WATCH_HISTORY_GRID_ROWS = 4

/**
 * Календарь последних 4 недель: колонки — дни недели начиная с [firstDayOfWeek],
 * последняя строка — текущая неделя. Пропущенные сервером дни заполняются нулями.
 */
fun List<UserWatchHistoryDay>.toWatchHistoryGrid(
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
): ImmutableList<WatchHistoryCell> {
    val byDate = groupBy { Instant.ofEpochSecond(it.dateSeconds).atZone(zone).toLocalDate() }
    val start = today
        .with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        .minusWeeks((WATCH_HISTORY_GRID_ROWS - 1).toLong())
    val days = (0 until WATCH_HISTORY_GRID_COLUMNS * WATCH_HISTORY_GRID_ROWS).map { offset ->
        val date = start.plusDays(offset.toLong())
        val entries = byDate[date].orEmpty()
        Triple(
            date,
            entries.sumOf { it.durationSeconds.coerceAtLeast(0L) },
            entries.sumOf { it.episodeCount.coerceAtLeast(0) },
        )
    }
    val maxDuration = days.maxOf { it.second }.coerceAtLeast(1L)
    return days.map { (date, duration, episodes) ->
        WatchHistoryCell(
            date = date,
            durationSeconds = duration,
            episodeCount = episodes,
            intensity = if (duration <= 0L) {
                0f
            } else {
                (MIN_INTENSITY + (1f - MIN_INTENSITY) * duration.toFloat() / maxDuration).coerceIn(MIN_INTENSITY, 1f)
            },
            isToday = date == today,
            isFuture = date.isAfter(today),
        )
    }.toImmutableList()
}

private const val MIN_INTENSITY = 0.32f

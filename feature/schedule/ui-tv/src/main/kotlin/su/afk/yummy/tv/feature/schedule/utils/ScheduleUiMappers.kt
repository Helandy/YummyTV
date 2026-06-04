package su.afk.yummy.tv.feature.schedule.utils

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi
import su.afk.yummy.tv.feature.schedule.model.ScheduleReleaseUi
import su.afk.yummy.tv.feature.schedule.model.ScheduleRemainingLabels
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

internal fun List<AnimeScheduleDay>.toUiDayGroups(
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

internal fun ZonedDateTime.timeLabel(): String =
    "%02d:%02d".format(hour, minute)

internal fun ZonedDateTime.remainingText(
    now: ZonedDateTime,
    labels: ScheduleRemainingLabels,
): String {
    val duration = Duration.between(now, this).coerceAtLeast(Duration.ZERO)
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    return when {
        days > 0 -> listOfNotNull(
            days.toInt().ruUnit(labels.dayOne, labels.dayFew, labels.dayMany),
            hours.takeIf { it > 0 }?.toInt()?.ruUnit(labels.hourOne, labels.hourFew, labels.hourMany),
        ).joinToString(" ")
        hours > 0 -> listOfNotNull(
            hours.toInt().ruUnit(labels.hourOne, labels.hourFew, labels.hourMany),
            minutes.takeIf { it > 0 }?.toInt()?.ruUnit(labels.minuteOne, labels.minuteFew, labels.minuteMany),
        ).joinToString(" ")
        minutes > 0 -> minutes.toInt().ruUnit(labels.minuteOne, labels.minuteFew, labels.minuteMany)
        else -> labels.lessThanMinute
    }
}

private fun Int.ruUnit(one: String, few: String, many: String): String {
    val mod100 = this % 100
    val mod10 = this % 10
    val unit = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$this $unit"
}

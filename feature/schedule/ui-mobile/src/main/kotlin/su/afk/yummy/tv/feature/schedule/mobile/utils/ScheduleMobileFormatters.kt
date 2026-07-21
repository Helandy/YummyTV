package su.afk.yummy.tv.feature.schedule.mobile.utils

import su.afk.yummy.tv.feature.schedule.mobile.model.ScheduleMobileRemainingLabels
import java.time.Duration
import java.time.ZonedDateTime

internal fun ZonedDateTime.timeLabel(): String =
    "%02d:%02d".format(hour, minute)

internal fun ZonedDateTime.remainingText(
    now: ZonedDateTime,
    labels: ScheduleMobileRemainingLabels,
): String {
    val duration = Duration.between(now, this).coerceAtLeast(Duration.ZERO)
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    return when {
        days > 0 -> listOfNotNull(
            days.toInt().ruUnit(labels.dayOne, labels.dayFew, labels.dayMany),
            hours.takeIf { it > 0 }?.toInt()
                ?.ruUnit(labels.hourOne, labels.hourFew, labels.hourMany),
        ).joinToString(" ")

        hours > 0 -> listOfNotNull(
            hours.toInt().ruUnit(labels.hourOne, labels.hourFew, labels.hourMany),
            minutes.takeIf { it > 0 }?.toInt()
                ?.ruUnit(labels.minuteOne, labels.minuteFew, labels.minuteMany),
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

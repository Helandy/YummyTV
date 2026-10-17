package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Номер следующей серии: вышедшие + 1. `null`, если число вышедших неизвестно или все серии уже
 * вышли.
 */
fun AnimeEpisodes.nextEpisodeNumber(): Int? {
    val next = (aired ?: return null) + 1
    val total = count?.takeIf { it > 0 } ?: return next
    return next.takeIf { it <= total }
}

/**
 * Время выхода по часам устройства: для серии в ближайшие [WEEKDAY_WINDOW_DAYS] дней — «пт, 18:00»,
 * для более далёкой — «22 янв, 03:00». День недели без даты у далёкого выхода читается как
 * ближайшая пятница.
 */
fun Long.formatReleaseWeekdayTime(
    nowEpochSeconds: Long,
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val withinWindow = this - nowEpochSeconds < WEEKDAY_WINDOW_DAYS * SECONDS_PER_DAY
    val pattern = if (withinWindow) RELEASE_WEEKDAY_TIME_PATTERN else RELEASE_DATE_TIME_PATTERN
    // Короткие месяцы в ru/uk идут с точкой («янв.») — перед запятой она лишняя.
    return DateTimeFormatter.ofPattern(pattern, locale)
        .format(Instant.ofEpochSecond(this).atZone(zone))
        .replace(".,", ",")
}

private const val WEEKDAY_WINDOW_DAYS = 7L
private const val SECONDS_PER_DAY = 86_400L
private const val RELEASE_WEEKDAY_TIME_PATTERN = "EE, HH:mm"
private const val RELEASE_DATE_TIME_PATTERN = "d MMM, HH:mm"

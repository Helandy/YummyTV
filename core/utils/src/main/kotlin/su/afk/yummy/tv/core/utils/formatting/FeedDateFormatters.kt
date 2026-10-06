package su.afk.yummy.tv.core.utils.formatting

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Единый формат даты для лент (рецензии, посты, видео блогеров): «день месяц, часы:минуты».
 * Вход — Unix-время в секундах. Единый источник, чтобы не плодить inline-[java.text.DateFormat] по фичам.
 */
fun Long.formatFeedDateTime(): String =
    SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault()).format(Date(this * 1_000L))

/**
 * Дата выхода серии для карточек: «3 окт» или «3 окт, 17:30», если время известно. Месяц короткий —
 * подпись живёт в узкой карточке. Вход — Unix-время в секундах.
 *
 * Ровная полночь UTC означает, что источник знает только день: у yani половина дат в расписании
 * приходит именно так. Такие даты и форматируем в UTC — в локальной зоне западнее Гринвича полночь
 * съезжает на предыдущий день, а время показывать нельзя, оно выглядело бы настоящим (03:00 для MSK).
 */
fun Long.formatAirDate(): String {
    val dayOnly = this % SECONDS_IN_DAY == 0L
    val format = SimpleDateFormat(if (dayOnly) "d MMM" else "d MMM, HH:mm", Locale.getDefault())
    if (dayOnly) format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(this * 1_000L))
}

private const val SECONDS_IN_DAY = 24L * 60L * 60L

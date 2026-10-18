package su.afk.yummy.tv.feature.account.common.utils

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.common.model.WatchHistoryCell
import su.afk.yummy.tv.feature.account.presentation.R
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Полное описание дня одной строкой — для озвучки ячейки (TalkBack). */
@Composable
fun WatchHistoryCell.detailsLabel(): String {
    val parts = buildList {
        add(dayLabel())
        if (!hasWatches) {
            add(stringResource(R.string.account_mobile_heatmap_no_watches))
        } else {
            durationLabel()?.let(::add)
            episodesLabel()?.let(::add)
        }
    }
    return parts.joinToString(separator = ", ")
}

val WatchHistoryCell.hasWatches: Boolean
    get() = durationSeconds > 0L || episodeCount > 0

/** «18 окт.» — заголовок блока выбранного дня. */
fun WatchHistoryCell.dayTitle(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale).format(date)

/** «воскресенье» — подпись под датой. */
fun WatchHistoryCell.weekdayName(locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale)

/** «1 ч 20 мин», «21 мин»; null для дня без времени просмотра. */
@Composable
fun WatchHistoryCell.durationLabel(): String? =
    durationSeconds.takeIf { it > 0L }?.watchDurationLabel()

/** «7 серий»; null для дня без серий. */
@Composable
fun WatchHistoryCell.episodesLabel(): String? =
    episodeCount.takeIf { it > 0 }?.let {
        pluralStringResource(R.plurals.account_mobile_heatmap_episodes, it, it)
    }

/** Короткие подписи дней недели («пн», «вт», …) в порядке колонок календаря. */
fun DayOfWeek.weekdayLabels(locale: Locale = Locale.getDefault()): List<String> =
    (0 until DAYS_IN_WEEK).map { plus(it.toLong()).getDisplayName(TextStyle.SHORT_STANDALONE, locale) }

private fun WatchHistoryCell.dayLabel(): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

@Composable
private fun Long.watchDurationLabel(): String {
    val totalMinutes = (this / SECONDS_IN_MINUTE).coerceAtLeast(1L)
    val hours = totalMinutes / MINUTES_IN_HOUR
    val minutes = totalMinutes % MINUTES_IN_HOUR
    return when {
        hours == 0L -> stringResource(R.string.account_mobile_heatmap_duration_minutes, minutes)
        minutes == 0L -> stringResource(R.string.account_profile_duration_hours, hours)
        else -> stringResource(R.string.account_mobile_heatmap_duration_hours_minutes, hours, minutes)
    }
}

private const val DAYS_IN_WEEK = 7
private const val SECONDS_IN_MINUTE = 60L
private const val MINUTES_IN_HOUR = 60L

package su.afk.yummy.tv.core.utils.formatting

import java.util.Locale

/** Таймкод по миллисекундам: `m:ss`, а для длинных значений — `h:mm:ss`. Отрицательные значения считаются нулём. */
fun Long.millisToClockTime(): String =
    (coerceAtLeast(0L) / MILLIS_IN_SECOND).toInt().secondsToClockTime()

/** Таймкод по секундам: `m:ss`, а для длинных значений — `h:mm:ss`. Отрицательные значения считаются нулём. */
fun Int.secondsToClockTime(): String {
    val totalSeconds = coerceAtLeast(0)
    val hours = totalSeconds / SECONDS_IN_HOUR
    val minutes = totalSeconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE
    val seconds = totalSeconds % SECONDS_IN_MINUTE
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private const val MILLIS_IN_SECOND = 1_000L
private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_IN_HOUR = 3_600

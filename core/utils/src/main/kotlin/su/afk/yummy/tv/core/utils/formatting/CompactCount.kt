package su.afk.yummy.tv.core.utils.formatting

import java.util.Locale

/**
 * Formats a count into a short, app-wide consistent form, e.g. 1100 -> "1.1K", 2_000_000 -> "2M".
 * Matches the abbreviation style used on the details screens.
 */
fun Int.toCompactCount(): String = toLong().toCompactCount()

fun Long.toCompactCount(): String = when {
    this >= 1_000_000 -> "${(this / 1_000_000f).toCompactDecimal()}M"
    this >= 1_000 -> "${(this / 1_000f).toCompactDecimal()}K"
    else -> toString()
}

/** Число без дробной части, если она нулевая, иначе с одним знаком: `2`, `1.1`. */
fun Float.toCompactDecimal(): String =
    if (this % 1f == 0f) toInt().toString() else String.format(Locale.US, "%.1f", this)

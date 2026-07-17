package su.afk.yummy.tv.core.utils

import java.util.Locale

/**
 * Formats a count into a short, app-wide consistent form, e.g. 1100 -> "1.1K", 2_000_000 -> "2M".
 * Matches the abbreviation style used on the details screens.
 */
fun Int.toCompactCount(): String = toLong().toCompactCount()

fun Long.toCompactCount(): String = when {
    this >= 1_000_000 -> "${(this / 1_000_000f).compactDecimal()}M"
    this >= 1_000 -> "${(this / 1_000f).compactDecimal()}K"
    else -> toString()
}

private fun Float.compactDecimal(): String =
    if (this % 1f == 0f) toInt().toString() else String.format(Locale.US, "%.1f", this)

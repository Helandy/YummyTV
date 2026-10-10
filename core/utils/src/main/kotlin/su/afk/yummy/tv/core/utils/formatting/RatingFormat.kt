package su.afk.yummy.tv.core.utils.formatting

import java.util.Locale

/** Оценка с одним знаком после запятой, усечением без округления: `8.97` -> `8.9`. */
fun Double.formatRating(): String {
    val truncated = (this * 10).toInt() / 10.0
    return String.format(Locale.US, "%.1f", truncated)
}

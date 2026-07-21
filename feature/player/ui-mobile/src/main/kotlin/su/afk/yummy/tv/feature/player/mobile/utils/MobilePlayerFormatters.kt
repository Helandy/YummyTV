package su.afk.yummy.tv.feature.player.mobile.utils

import java.util.Locale
import kotlin.math.roundToInt

internal fun formatMobilePlayerTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

internal fun Float.zoomIndicatorLabel(): String {
    val tenths = (coerceAtLeast(1f) * 10f).roundToInt()
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0) {
        "x$whole"
    } else {
        "x$whole.$fraction"
    }
}

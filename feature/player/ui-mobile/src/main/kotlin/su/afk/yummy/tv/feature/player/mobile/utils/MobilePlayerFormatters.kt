package su.afk.yummy.tv.feature.player.mobile.utils

import kotlin.math.roundToInt

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

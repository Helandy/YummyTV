package su.afk.yummy.tv.feature.details.mobile.episodes.utils

import su.afk.yummy.tv.core.utils.player.playerDisplayName
import java.util.Locale

internal fun String.playerLabel(): String = playerDisplayName()

internal fun Long.formatMegabytesOrNull(): String? {
    if (this <= 0L) return null
    val megabytes = toDouble() / (1024.0 * 1024.0)
    return if (megabytes < 100.0) {
        String.format(Locale.US, "%.1f MB", megabytes)
    } else {
        String.format(Locale.US, "%.0f MB", megabytes)
    }
}

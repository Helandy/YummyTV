package su.afk.yummy.tv.feature.videodownload.utils

import java.util.Locale

internal fun Long.formatMegabytesOrNull(): String? {
    if (this <= 0L) return null
    val megabytes = this.toDouble() / BYTES_IN_MEGABYTE
    return if (megabytes < 100.0) {
        String.format(Locale.US, "%.1f MB", megabytes)
    } else {
        String.format(Locale.US, "%.0f MB", megabytes)
    }
}

private const val BYTES_IN_MEGABYTE = 1024.0 * 1024.0

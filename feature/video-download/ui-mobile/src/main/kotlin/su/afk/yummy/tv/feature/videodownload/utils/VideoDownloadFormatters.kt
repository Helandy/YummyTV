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

internal fun Long.formatDiskSize(): String {
    val megabytes = coerceAtLeast(0L).toDouble() / BYTES_IN_MEGABYTE
    if (megabytes < MEGABYTES_IN_GIGABYTE) {
        return if (megabytes == 0.0) {
            "0 MB"
        } else if (megabytes < 100.0) {
            String.format(Locale.US, "%.1f MB", megabytes)
        } else {
            String.format(Locale.US, "%.0f MB", megabytes)
        }
    }

    return String.format(Locale.US, "%.1f GB", megabytes / MEGABYTES_IN_GIGABYTE)
}

private const val BYTES_IN_MEGABYTE = 1024.0 * 1024.0
private const val MEGABYTES_IN_GIGABYTE = 1024.0

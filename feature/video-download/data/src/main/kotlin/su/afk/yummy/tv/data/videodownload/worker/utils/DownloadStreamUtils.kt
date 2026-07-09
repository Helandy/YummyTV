package su.afk.yummy.tv.data.videodownload.worker.utils

internal enum class StreamKind {
    Hls,
    Dash,
    Progressive;

    val isAdaptive: Boolean
        get() = this == Hls || this == Dash
}

internal fun String.streamKind(): StreamKind {
    val cleanUrl = cleanStreamUrl()
    return when {
        cleanUrl.endsWith(".m3u8", ignoreCase = true) -> StreamKind.Hls
        cleanUrl.endsWith(".mpd", ignoreCase = true) -> StreamKind.Dash
        else -> StreamKind.Progressive
    }
}

internal fun StreamKind.throttleLabel(): String = "off"

internal fun StreamKind.segmentPaceLabel(isAlloha: Boolean): String =
    if (this == StreamKind.Hls && isAlloha) "${ALLOHA_HLS_REQUEST_INTERVAL_MS}ms" else "off"

internal const val ALLOHA_HLS_REQUEST_INTERVAL_MS = 4_000L

private fun String.cleanStreamUrl(): String =
    substringBefore('?').substringBefore('#')

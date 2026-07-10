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

private fun String.cleanStreamUrl(): String =
    substringBefore('?').substringBefore('#')

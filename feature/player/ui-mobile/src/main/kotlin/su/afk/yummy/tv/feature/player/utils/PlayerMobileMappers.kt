package su.afk.yummy.tv.feature.player.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips

internal fun PlayerSkips.segments(): List<Pair<String, PlayerSkipSegment>> =
    buildList {
        opening?.let { add("opening" to it) }
        ending?.let { add("ending" to it) }
    }

internal fun mediaItemFor(url: String): MediaItem {
    val lower = url.lowercase()
    val mimeType = when {
        ".m3u8" in lower -> MimeTypes.APPLICATION_M3U8
        ".mpd" in lower -> MimeTypes.APPLICATION_MPD
        else -> null
    }
    return MediaItem.Builder()
        .setUri(url)
        .setMimeType(mimeType)
        .build()
}

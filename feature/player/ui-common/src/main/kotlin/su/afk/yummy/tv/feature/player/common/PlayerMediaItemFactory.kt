package su.afk.yummy.tv.feature.player.common

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

object PlayerMediaItemFactory {
    fun mediaItemFor(url: String): MediaItem {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val mimeType = when {
            cleanUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            cleanUrl.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
            else -> null
        }
        return MediaItem.Builder()
            .setUri(url)
            .apply { if (mimeType != null) setMimeType(mimeType) }
            .build()
    }
}

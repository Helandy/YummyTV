package su.afk.yummy.tv.feature.player.common

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes

object PlayerMediaItemFactory {
    fun mediaItemFor(
        url: String,
        title: String? = null,
        artist: String? = null,
        subtitle: String? = null,
        description: String? = null,
        artworkUri: String? = null,
        durationMs: Long? = null,
        customCacheKey: String? = null,
    ): MediaItem {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val mimeType = when {
            cleanUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            cleanUrl.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
            else -> null
        }
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title.nonBlank())
            .setArtist(artist.nonBlank())
            .setSubtitle(subtitle.nonBlank())
            .setDescription(description.nonBlank())
            .setArtworkUri(artworkUri.nonBlank()?.let(Uri::parse))
            .setDurationMs(durationMs?.takeIf { it > 0L })
            .build()
        return MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(mediaMetadata)
            .setCustomCacheKey(customCacheKey.nonBlank())
            .apply { if (mimeType != null) setMimeType(mimeType) }
            .build()
    }
}

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

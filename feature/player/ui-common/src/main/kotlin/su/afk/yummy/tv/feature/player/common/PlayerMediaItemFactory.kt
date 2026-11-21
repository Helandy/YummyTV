package su.afk.yummy.tv.feature.player.common

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import su.afk.yummy.tv.feature.player.utils.isSyntheticMasterUrl

object PlayerMediaItemFactory {
    fun mediaItemFor(
        url: String,
        mediaId: String? = null,
        title: String? = null,
        artist: String? = null,
        subtitle: String? = null,
        description: String? = null,
        artworkUri: String? = null,
        durationMs: Long? = null,
        customCacheKey: String? = null,
        subtitleUrl: String? = null,
        subtitleMimeType: String? = null,
        subtitleLanguage: String? = null,
        subtitleLabel: String? = null,
    ): MediaItem {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val mimeType = when {
            url.isSyntheticMasterUrl() -> MimeTypes.APPLICATION_M3U8
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
            .apply { mediaId.nonBlank()?.let(::setMediaId) }
            .setMediaMetadata(mediaMetadata)
            .setCustomCacheKey(customCacheKey.nonBlank())
            .apply { if (mimeType != null) setMimeType(mimeType) }
            .apply {
                // Alloha ships subtitles as a standalone file rather than an in-stream track, so
                // they are side-loaded here and selected via SELECTION_FLAG_DEFAULT.
                subtitleUrl.nonBlank()?.let { url ->
                    setSubtitleConfigurations(
                        listOf(
                            MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                                .setMimeType(subtitleMimeType ?: MimeTypes.TEXT_VTT)
                                .setLanguage(subtitleLanguage.nonBlank())
                                .setLabel(subtitleLabel.nonBlank())
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        )
                    )
                }
            }
            .build()
    }
}

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

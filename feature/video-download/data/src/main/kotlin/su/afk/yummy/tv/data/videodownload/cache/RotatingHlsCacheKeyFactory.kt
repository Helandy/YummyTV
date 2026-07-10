package su.afk.yummy.tv.data.videodownload.cache

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory

/**
 * Keeps Alloha HLS media segments reusable when a signed manifest URL is rotated.
 *
 * Playlists deliberately retain their Media3 URL/custom keys so every rotation reads the fresh
 * signed segment list. Media files use a per-download namespace plus their stable file name; cache
 * ranges still distinguish byte-range based segments within the same resource.
 */
@OptIn(UnstableApi::class)
class RotatingHlsCacheKeyFactory(
    private val downloadCacheKey: String,
) : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec): String {
        val defaultKey = CacheKeyFactory.DEFAULT.buildCacheKey(dataSpec)
        if (dataSpec.key != null) return defaultKey

        val fileName = dataSpec.uri.lastPathSegment
            ?.substringBefore('?')
            ?.takeIf { it.isNotBlank() }
            ?: return defaultKey
        if (!fileName.hasStableHlsMediaExtension()) return defaultKey

        return resourcePrefix(downloadCacheKey) + fileName
    }

    private fun String.hasStableHlsMediaExtension(): Boolean =
        STABLE_HLS_MEDIA_EXTENSIONS.any { extension -> endsWith(extension, ignoreCase = true) }

    companion object {
        fun resourcePrefix(downloadCacheKey: String): String =
            "$downloadCacheKey$ALLOHA_SEGMENT_SEPARATOR"

        private const val ALLOHA_SEGMENT_SEPARATOR = "|alloha-segment|"
        private val STABLE_HLS_MEDIA_EXTENSIONS = listOf(
            ".m4s",
            ".ts",
            ".aac",
            ".mp4",
            ".key",
        )
    }
}

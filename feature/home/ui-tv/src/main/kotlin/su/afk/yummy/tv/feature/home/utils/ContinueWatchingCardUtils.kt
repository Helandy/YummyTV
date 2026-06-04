package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor

internal suspend fun resolveEpisodeThumbnail(entry: WatchProgressEntry): String? {
    val screenshotSource = entry.screenshotUrl.takeIf { it.isKodikSourceUrl() }
    return screenshotSource?.let { KodikThumbnailExtractor.extract(it) }
        ?: entry.episodeUrl.takeIf { it.isNotBlank() }?.let { KodikThumbnailExtractor.extract(it) }
}

internal fun String.isLikelyImageUrl(): Boolean =
    Regex("""\.(webp|avif|jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

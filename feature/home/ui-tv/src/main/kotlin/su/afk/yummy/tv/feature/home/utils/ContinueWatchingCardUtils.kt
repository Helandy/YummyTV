package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

internal suspend fun resolveEpisodeThumbnail(entry: HomeContinueWatchingItem): String? {
    val screenshotSource = entry.screenshotUrl.takeIf { it.isKodikSourceUrl() }
    val episodeSource = entry.episodeUrl.takeIf { it.isKodikSourceUrl() }
    return screenshotSource?.let { KodikThumbnailExtractor.extract(it) }
        ?: episodeSource?.let { KodikThumbnailExtractor.extract(it) }
}

internal fun String.isLikelyImageUrl(): Boolean =
    Regex("""\.(webp|avif|jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

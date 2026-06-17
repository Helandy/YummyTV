package su.afk.yummy.tv.core.utils

suspend fun resolveContinueWatchingImage(
    screenshotUrl: String,
    episodeUrl: String,
    posterUrl: String?,
): String? {
    val kodikScreenshot = screenshotUrl.takeIf { it.isKodikSourceUrl() }
    val kodikEpisode = episodeUrl.takeIf { it.isKodikSourceUrl() }
    val directScreenshot = screenshotUrl.takeIf { it.isLikelyImageUrl() }
    return kodikScreenshot?.let { KodikThumbnailExtractor.extract(it) }
        ?: kodikEpisode?.let { KodikThumbnailExtractor.extract(it) }
        ?: directScreenshot
        ?: posterUrl
}

fun String.isLikelyImageUrl(): Boolean =
    Regex("""\.(webp|avif|jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

package su.afk.yummy.tv.core.utils

suspend fun resolveContinueWatchingImage(
    screenshotUrl: String,
    episodeUrl: String,
    posterUrl: String?,
    resolveKodikThumbnail: suspend (iframeUrl: String) -> String?,
): String? {
    val kodikScreenshot = screenshotUrl.takeIf { it.isKodikSourceUrl() }
    val kodikEpisode = episodeUrl.takeIf { it.isKodikSourceUrl() }
    val directScreenshot = screenshotUrl.takeIf { it.isLikelyImageUrl() }
    return kodikScreenshot?.let { resolveKodikThumbnail(it) }
        ?: kodikEpisode?.let { resolveKodikThumbnail(it) }
        ?: directScreenshot
        ?: posterUrl
}

fun resolveContinueWatchingImageModel(
    screenshotUrl: String,
    episodeUrl: String,
    posterUrl: String?,
    kodikThumbnailModel: (iframeUrl: String) -> Any?,
): Any? {
    val kodikScreenshot = screenshotUrl.takeIf { it.isKodikSourceUrl() }
    val kodikEpisode = episodeUrl.takeIf { it.isKodikSourceUrl() }
    val directScreenshot = screenshotUrl.takeIf { it.isLikelyImageUrl() }
    return kodikScreenshot?.let(kodikThumbnailModel)
        ?: kodikEpisode?.let(kodikThumbnailModel)
        ?: directScreenshot
        ?: posterUrl
}

fun String.isLikelyImageUrl(): Boolean =
    Regex("""\.(webp|avif|jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

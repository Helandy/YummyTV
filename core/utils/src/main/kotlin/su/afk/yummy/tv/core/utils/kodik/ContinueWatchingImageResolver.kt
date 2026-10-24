package su.afk.yummy.tv.core.utils.kodik

import su.afk.yummy.tv.core.utils.network.isLikelyImageUrl

private enum class ContinueWatchingImageSource { KODIK_SCREENSHOT, KODIK_EPISODE, DIRECT_SCREENSHOT, POSTER }

private fun pickContinueWatchingImageSource(
    screenshotUrl: String,
    episodeUrl: String,
    posterUrl: String?,
): Pair<ContinueWatchingImageSource, String>? = when {
    screenshotUrl.isKodikIframeUrl() -> ContinueWatchingImageSource.KODIK_SCREENSHOT to screenshotUrl
    episodeUrl.isKodikIframeUrl() -> ContinueWatchingImageSource.KODIK_EPISODE to episodeUrl
    screenshotUrl.isLikelyImageUrl() -> ContinueWatchingImageSource.DIRECT_SCREENSHOT to screenshotUrl
    posterUrl != null -> ContinueWatchingImageSource.POSTER to posterUrl
    else -> null
}

suspend fun resolveContinueWatchingImage(
    screenshotUrl: String,
    episodeUrl: String,
    posterUrl: String?,
    resolveKodikThumbnail: suspend (iframeUrl: String) -> String?,
): String? {
    val (source, url) = pickContinueWatchingImageSource(screenshotUrl, episodeUrl, posterUrl)
        ?: return null
    return when (source) {
        ContinueWatchingImageSource.KODIK_SCREENSHOT,
        ContinueWatchingImageSource.KODIK_EPISODE,
            -> resolveKodikThumbnail(url)

        ContinueWatchingImageSource.DIRECT_SCREENSHOT,
        ContinueWatchingImageSource.POSTER,
            -> url
    }
}

/**
 * Цепочка моделей картинки «продолжить просмотр» по убыванию предпочтения: превью серии
 * (скриншот или Kodik), если оно есть, затем все версии постера из [posterUrls]. Если первая модель
 * не загрузилась, [su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage] берёт следующую.
 */
fun resolveContinueWatchingImageModels(
    screenshotUrl: String,
    episodeUrl: String,
    posterUrls: List<String>,
    kodikThumbnailModel: (iframeUrl: String) -> Any?,
): List<Any> {
    val episodeModel = when {
        screenshotUrl.isKodikIframeUrl() -> kodikThumbnailModel(screenshotUrl)
        episodeUrl.isKodikIframeUrl() -> kodikThumbnailModel(episodeUrl)
        screenshotUrl.isLikelyImageUrl() -> screenshotUrl
        else -> null
    }
    return listOfNotNull(episodeModel) + posterUrls
}

fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

/**
 * Kodik-iframe, из HTML которого достаётся превью. Скриншоты с `i.kodikres.com` тоже содержат
 * «kodik», но это уже готовые картинки — их грузим напрямую.
 */
fun String.isKodikIframeUrl(): Boolean = isKodikSourceUrl() && !isLikelyImageUrl()

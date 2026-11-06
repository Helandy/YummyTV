package su.afk.yummy.tv.core.model.settings

/**
 * Выбирает стартовое качество постеров по фактически подтверждённой поддержке форматов:
 * Качество постеров задаётся только поддержкой форматов устройством, пользователь его не выбирает.
 * AVIF → [PosterQuality.MEGA], иначе WebP → [PosterQuality.STANDARD], иначе JPEG → [PosterQuality.HIGH].
 */
fun choosePosterQuality(avifSupported: Boolean, webpSupported: Boolean): PosterQuality = when {
    avifSupported -> PosterQuality.MEGA
    webpSupported -> PosterQuality.STANDARD
    else -> PosterQuality.HIGH
}

/** Качество постеров для этого устройства: проба форматов в [ImageFormatSupport] выполняется один раз. */
suspend fun ImageFormatSupport.choosePosterQuality(): PosterQuality = choosePosterQuality(
    avifSupported = isAvifSupported(),
    webpSupported = isWebpSupported(),
)

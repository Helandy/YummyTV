package su.afk.yummy.tv.core.model.settings

/**
 * Выбирает стартовое качество постеров по фактически подтверждённой поддержке форматов:
 * AVIF → [PosterQuality.MEGA], иначе WebP → [PosterQuality.STANDARD], иначе JPEG → [PosterQuality.HIGH].
 */
fun choosePosterQuality(avifSupported: Boolean, webpSupported: Boolean): PosterQuality = when {
    avifSupported -> PosterQuality.MEGA
    webpSupported -> PosterQuality.STANDARD
    else -> PosterQuality.HIGH
}

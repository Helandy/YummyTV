package su.afk.yummy.tv.core.utils

/**
 * Модель для Coil: превью серии по kodik-iframe.
 * Coil Keyer/Fetcher используют iframe как стабильный ключ memory/disk cache.
 */
data class KodikThumbnail(val iframeUrl: String) {
    val cacheKey: String
        get() = "kodik_thumb:" + normalizeIframeUrl(iframeUrl)
}

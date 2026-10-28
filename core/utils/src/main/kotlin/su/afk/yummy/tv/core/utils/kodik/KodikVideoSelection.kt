package su.afk.yummy.tv.core.utils.kodik

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.network.isLikelyImageUrl

/**
 * Выбор Kodik-видео для превьюшек серий. Живёт в core:utils, а не в core:model: знание о
 * конкретном балансере — деталь реализации превью, а shared kernel должен оставаться нейтральным.
 */

/** Iframe выбранной озвучки, иначе самой просматриваемой — из него достаётся картинка серии. */
fun List<AnimeVideo>.kodikThumbnailIframeUrl(preferredDubbing: String = ""): String? {
    val kodikVideos = filter { it.isKodikSource() }
    return kodikVideos.firstOrNull {
        preferredDubbing.isNotBlank() && it.dubbing == preferredDubbing
    }?.iframeUrl ?: kodikVideos.maxByOrNull { it.views ?: 0 }?.iframeUrl
}

fun AnimeVideo.isKodikSource(): Boolean =
    player.isKodikSourceUrl() || iframeUrl.isKodikSourceUrl()

fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

/**
 * Kodik-iframe, из HTML которого достаётся превью. Скриншоты с `i.kodikres.com` тоже содержат
 * «kodik», но это уже готовые картинки — их грузим напрямую.
 */
fun String.isKodikIframeUrl(): Boolean = isKodikSourceUrl() && !isLikelyImageUrl()

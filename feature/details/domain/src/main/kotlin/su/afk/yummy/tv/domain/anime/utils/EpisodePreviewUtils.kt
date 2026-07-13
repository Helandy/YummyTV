package su.afk.yummy.tv.domain.anime.utils

import su.afk.yummy.tv.domain.anime.model.AnimeVideo

fun List<AnimeVideo>.bestKodikDubbing(): String {
    val kodikVideos = filter { it.isKodikSource() }
    return kodikVideos
        .groupBy { it.dubbing }
        .maxByOrNull { (_, videos) -> videos.sumOf { it.views ?: 0 } }
        ?.key
        .orEmpty()
}

fun List<AnimeVideo>.kodikThumbnailIframeUrl(preferredDubbing: String = ""): String? {
    val kodikVideos = filter { it.isKodikSource() }
    return kodikVideos.firstOrNull {
        preferredDubbing.isNotBlank() && it.dubbing == preferredDubbing
    }?.iframeUrl ?: kodikVideos.maxByOrNull { it.views ?: 0 }?.iframeUrl
}

fun AnimeVideo.isKodikSource(): Boolean =
    player.contains(KODIK_SOURCE_MARKER, ignoreCase = true) ||
            iframeUrl.contains(KODIK_SOURCE_MARKER, ignoreCase = true)

private const val KODIK_SOURCE_MARKER = "kodik"

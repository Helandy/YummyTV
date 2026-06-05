package su.afk.yummy.tv.feature.details.utils

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
    return kodikVideos.firstOrNull { preferredDubbing.isNotBlank() && it.dubbing == preferredDubbing }
        ?.iframeUrl
        ?: kodikVideos.maxByOrNull { it.views ?: 0 }?.iframeUrl
}

fun AnimeVideo.isKodikSource(): Boolean =
    player.contains("kodik", ignoreCase = true) ||
            iframeUrl.contains("kodik", ignoreCase = true)

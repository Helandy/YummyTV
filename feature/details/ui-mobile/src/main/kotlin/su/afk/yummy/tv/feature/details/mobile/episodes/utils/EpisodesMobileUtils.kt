package su.afk.yummy.tv.feature.details.mobile.episodes.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.kodik.isKodikSource

internal fun List<AnimeVideo>.representativeVideo(bestDubbing: String): AnimeVideo {
    val kodikVideos = filter { it.isKodikSource() }
    val source = kodikVideos.ifEmpty { this }
    return source.firstOrNull { bestDubbing.isNotBlank() && it.dubbing == bestDubbing }
        ?: source.maxByOrNull { it.views ?: 0 }
        ?: first()
}

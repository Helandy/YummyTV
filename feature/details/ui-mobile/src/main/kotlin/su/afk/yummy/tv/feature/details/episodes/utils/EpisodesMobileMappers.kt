package su.afk.yummy.tv.feature.details.episodes.utils

import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.model.MobileEpisodeGroup

internal fun List<AnimeVideo>.toMobileEpisodeGroups(): List<MobileEpisodeGroup> {
    val bestKodikDubbing = bestKodikDubbing()
    return groupBy { it.episode }
        .entries
        .sortedWith(compareBy({ it.key.toIntOrNull() ?: 0 }, { it.key }))
        .map { (episode, videos) ->
            MobileEpisodeGroup(
                episode = episode,
                video = videos.representativeVideo(bestKodikDubbing),
            )
        }
}

private fun List<AnimeVideo>.bestKodikDubbing(): String {
    val kodikVideos = filter { it.isKodik() }
    return kodikVideos
        .groupBy { it.dubbing }
        .maxByOrNull { (_, videos) -> videos.sumOf { it.views ?: 0 } }
        ?.key
        .orEmpty()
}

private fun List<AnimeVideo>.representativeVideo(bestKodikDubbing: String): AnimeVideo {
    val kodikVideos = filter { it.isKodik() }
    val source = kodikVideos.ifEmpty { this }
    return source.firstOrNull { bestKodikDubbing.isNotBlank() && it.dubbing == bestKodikDubbing }
        ?: source.maxByOrNull { it.views ?: 0 }
        ?: first()
}

private fun AnimeVideo.isKodik(): Boolean =
    player.contains("kodik", ignoreCase = true) ||
        iframeUrl.contains("kodik", ignoreCase = true)

internal fun Int.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

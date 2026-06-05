package su.afk.yummy.tv.feature.details.episodes.utils

import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.model.MobileEpisodeGroup
import su.afk.yummy.tv.feature.details.utils.bestKodikDubbing
import su.afk.yummy.tv.feature.details.utils.isKodikSource
import su.afk.yummy.tv.feature.details.utils.kodikThumbnailIframeUrl

internal fun List<AnimeVideo>.toMobileEpisodeGroups(): List<MobileEpisodeGroup> {
    val bestKodikDubbing = bestKodikDubbing()
    return groupBy { it.episode }
        .entries
        .sortedWith(compareBy({ it.key.toIntOrNull() ?: 0 }, { it.key }))
        .map { (episode, videos) ->
            MobileEpisodeGroup(
                episode = episode,
                video = videos.representativeVideo(bestKodikDubbing),
                videos = videos,
                kodikIframeUrl = videos.kodikThumbnailIframeUrl(bestKodikDubbing),
            )
        }
}

private fun List<AnimeVideo>.representativeVideo(bestKodikDubbing: String): AnimeVideo {
    val kodikVideos = filter { it.isKodikSource() }
    val source = kodikVideos.ifEmpty { this }
    return source.firstOrNull { bestKodikDubbing.isNotBlank() && it.dubbing == bestKodikDubbing }
        ?: source.maxByOrNull { it.views ?: 0 }
        ?: first()
}

internal fun Int.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

package su.afk.yummy.tv.feature.details.details

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.utils.toPlayerVideoSource
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.isPlaceholderEpisode
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.selectContinueWatchingVideo

data class DetailsContinueTarget(
    val video: PlayerVideoSource,
)

fun resolveDetailsContinueTarget(
    animeId: Int,
    videos: List<AnimeVideo>,
    watchProgress: DetailsWatchProgressIndex,
): DetailsContinueTarget? {
    val latestEntry = watchProgress.latestMeaningful(animeId)
        ?: return null
    val videoSources = videos.map { it.toPlayerVideoSource() }
    if (videoSources.isEmpty()) return null

    val targetVideo = if (WatchProgressStore.isContinueWatchingEntry(latestEntry)) {
        videoSources.selectContinueWatchingVideo(
            videoId = latestEntry.videoId,
            episodeUrl = latestEntry.episodeUrl,
            episode = latestEntry.episode,
            playerName = latestEntry.playerName,
            dubbing = latestEntry.dubbing,
        ) ?: videoSources.firstOrNull()
    } else {
        videoSources.selectNextEpisodeAfter(latestEntry)
    } ?: return null

    return DetailsContinueTarget(video = targetVideo)
}

private fun List<PlayerVideoSource>.selectNextEpisodeAfter(entry: WatchProgressEntry): PlayerVideoSource? {
    val currentEpisode = findProgressVideo(entry)
        ?.episode
        ?.takeUnless { it.isPlaceholderEpisode() }
        ?: entry.episode.takeUnless { it.isPlaceholderEpisode() }
        ?: return null

    val candidateGroups = listOf(
        filter { it.player == entry.playerName && it.dubbing == entry.dubbing },
        filter { it.player == entry.playerName },
        filter { it.dubbing == entry.dubbing },
        this,
    )

    return candidateGroups.firstNotNullOfOrNull { group ->
        group.filter { it.iframeUrl.isSupportedPlayerUrl() }.nextAfter(currentEpisode)
    } ?: candidateGroups.firstNotNullOfOrNull { group ->
        group.nextAfter(currentEpisode)
    }
}

private fun List<PlayerVideoSource>.findProgressVideo(entry: WatchProgressEntry): PlayerVideoSource? =
    firstOrNull { entry.videoId > 0 && it.id == entry.videoId }
        ?: firstOrNull { entry.episodeUrl.isNotBlank() && it.iframeUrl == entry.episodeUrl }
        ?: firstOrNull {
            !entry.episode.isPlaceholderEpisode() &&
                    it.episode == entry.episode &&
                    it.player == entry.playerName &&
                    it.dubbing == entry.dubbing
        }
        ?: firstOrNull { !entry.episode.isPlaceholderEpisode() && it.episode == entry.episode }

private fun List<PlayerVideoSource>.nextAfter(episode: String): PlayerVideoSource? {
    if (isEmpty()) return null
    val sorted = sortedByEpisode()
    val currentIndex = sorted.indexOfLast { it.episode == episode }
    if (currentIndex >= 0) {
        return sorted.drop(currentIndex + 1).firstOrNull { it.episode != episode }
    }

    val currentNumber = episode.toDoubleOrNull() ?: return null
    return sorted.firstOrNull { next ->
        val nextNumber = next.episode.toDoubleOrNull()
        nextNumber != null && nextNumber > currentNumber
    }
}

private fun List<PlayerVideoSource>.sortedByEpisode(): List<PlayerVideoSource> =
    sortedWith(compareBy<PlayerVideoSource> {
        it.episode.toDoubleOrNull() ?: Double.MAX_VALUE
    }.thenBy { it.episode })

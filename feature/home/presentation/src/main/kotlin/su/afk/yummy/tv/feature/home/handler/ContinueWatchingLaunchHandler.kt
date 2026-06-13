package su.afk.yummy.tv.feature.home.handler

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.model.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest
import su.afk.yummy.tv.feature.player.isTrustedPlaceholderMigrationTarget
import su.afk.yummy.tv.feature.player.resolveContinueWatchingTarget
import javax.inject.Inject

internal class ContinueWatchingLaunchHandler @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val playerNavigator: IPlayerNavigator,
) {
    suspend fun getPlayerDestination(entry: WatchProgressEntry): NavKey {
        val videos = if (entry.animeId != 0) {
            runCatching { getAnimeVideos(entry.animeId) }.getOrNull().orEmpty()
        } else {
            emptyList()
        }

        val videoSources = videos.map { it.toPlayerVideoSource() }
        val progressVideo = entry.toPlayerVideoSource()
        val target = resolveContinueWatchingTarget(progressVideo, videoSources)
        migratePlaceholderEpisode(entry, progressVideo, target.video)

        return playerNavigator.getPlayerDest(
            video = target.video,
            allVideos = target.allVideos,
            animeTitle = entry.animeTitle,
            animeId = entry.animeId,
            posterUrl = entry.posterUrl,
        )
    }

    private suspend fun migratePlaceholderEpisode(
        entry: WatchProgressEntry,
        progressVideo: PlayerVideoSource,
        targetVideo: PlayerVideoSource,
    ) {
        if (!progressVideo.isTrustedPlaceholderMigrationTarget(targetVideo)) return
        watchProgressStore.save(
            animeId = entry.animeId,
            episode = targetVideo.episode,
            videoId = targetVideo.id,
            episodeUrl = targetVideo.iframeUrl,
            positionMs = entry.positionMs,
            durationMs = entry.durationMs,
            animeTitle = entry.animeTitle,
            posterUrl = entry.posterUrl,
            playerName = targetVideo.player,
            dubbing = targetVideo.dubbing,
            screenshotUrl = entry.screenshotUrl,
        )
        watchProgressStore.delete(entry.animeId, entry.episode)
    }
}

private fun AnimeVideo.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    iframeUrl = iframeUrl,
    views = views,
    skips = skips.toPlayerSkips(),
)

private fun WatchProgressEntry.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = videoId,
    episode = episode,
    dubbing = dubbing,
    player = playerName,
    iframeUrl = episodeUrl,
)

private fun AnimeVideoSkips.toPlayerSkips(): PlayerSkips = PlayerSkips(
    opening = opening.toPlayerSkipSegment(),
    ending = ending.toPlayerSkipSegment(),
)

private fun AnimeVideoSkipSegment?.toPlayerSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }

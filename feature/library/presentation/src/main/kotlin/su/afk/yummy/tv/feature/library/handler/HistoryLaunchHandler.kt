package su.afk.yummy.tv.feature.library.handler

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import javax.inject.Inject

class HistoryLaunchHandler @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val playerNavigator: IPlayerNavigator,
) {
    suspend fun destination(entry: WatchHistoryEntry): NavKey {
        val video = getAnimeVideos(entry.animeId).firstOrNull { it.id == entry.videoId }
            ?: throw HistoryVideoUnavailableException()
        return playerNavigator.getPlayerDest(
            iframeUrl = video.iframeUrl,
            animeTitle = entry.title,
            episode = video.episode,
            playerName = video.player,
            dubbing = video.dubbing,
            selectedVideoId = video.id,
            selectedPlayerId = video.playerId,
            selectedScreenshotUrl = entry.screenshotUrl.orEmpty(),
            animeId = entry.animeId,
            posterUrl = entry.posterUrl.orEmpty(),
            resumeFromMs = entry.positionSeconds.coerceAtLeast(0) * 1_000L,
        )
    }
}

private class HistoryVideoUnavailableException : IllegalStateException()

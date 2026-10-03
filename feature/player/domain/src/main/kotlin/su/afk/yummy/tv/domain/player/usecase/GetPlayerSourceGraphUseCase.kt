package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.PlayerSourceData
import su.afk.yummy.tv.domain.player.model.PlayerSourceGraph
import su.afk.yummy.tv.domain.player.model.PlayerSourceRequest
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import su.afk.yummy.tv.domain.player.utils.selectRequestedVideo
import su.afk.yummy.tv.domain.player.utils.toFallbackVideo
import su.afk.yummy.tv.domain.player.utils.toPlayerSourceGraph
import javax.inject.Inject

/** Собирает граф доступных источников плеера из навигационных данных и списка видео. */
class GetPlayerSourceGraphUseCase @Inject constructor(
    private val repository: PlayerSourceRepository,
) {
    suspend operator fun invoke(
        request: PlayerSourceRequest,
        forceRefreshVideos: Boolean = false,
    ): PlayerSourceGraph {
        val sourceData = if (request.animeId > 0) {
            repository.getSources(
                animeId = request.animeId,
                forceRefreshVideos = forceRefreshVideos,
            )
        } else {
            PlayerSourceData(emptyList())
        }
        val fallbackVideo = request.toFallbackVideo()
        val videos = sourceData.videos.ifEmpty { listOf(fallbackVideo) }
        val selectedVideo = videos.selectRequestedVideo(request) ?: fallbackVideo
        return videos.toPlayerSourceGraph(
            selectedVideo = selectedVideo,
            screenshotByEpisode = sourceData.screenshotByEpisode,
            fallbackScreenshotUrl = request.selectedScreenshotUrl,
        )
    }
}

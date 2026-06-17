package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.isKodikPlayerUrl
import su.afk.yummy.tv.domain.player.isSupportedPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerSourceBalancer
import su.afk.yummy.tv.domain.player.model.PlayerSourceData
import su.afk.yummy.tv.domain.player.model.PlayerSourceDubbing
import su.afk.yummy.tv.domain.player.model.PlayerSourceEpisode
import su.afk.yummy.tv.domain.player.model.PlayerSourceGraph
import su.afk.yummy.tv.domain.player.model.PlayerSourceRequest
import su.afk.yummy.tv.domain.player.model.PlayerSourceSelection
import su.afk.yummy.tv.domain.player.model.PlayerSourceVideo
import su.afk.yummy.tv.domain.player.playerDisplayOrderPriority
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import javax.inject.Inject

/** Собирает граф доступных источников плеера из навигационных данных и списка видео. */
class GetPlayerSourceGraphUseCase @Inject constructor(
    private val repository: PlayerSourceRepository,
) {
    suspend operator fun invoke(request: PlayerSourceRequest): PlayerSourceGraph {
        val sourceData = if (request.animeId > 0) {
            repository.getSources(request.animeId)
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

    private fun PlayerSourceRequest.toFallbackVideo(): PlayerSourceVideo =
        PlayerSourceVideo(
            id = selectedVideoId,
            episode = episode,
            dubbing = dubbing,
            player = playerName,
            playerId = selectedPlayerId,
            iframeUrl = iframeUrl,
        )

    private fun List<PlayerSourceVideo>.selectRequestedVideo(
        request: PlayerSourceRequest,
    ): PlayerSourceVideo? =
        firstOrNull { request.selectedVideoId > 0 && it.id == request.selectedVideoId }
            ?: firstOrNull { request.iframeUrl.isNotBlank() && it.iframeUrl == request.iframeUrl }
            ?: firstOrNull {
                request.episode.isNotBlank() &&
                        it.episode == request.episode &&
                        it.player == request.playerName &&
                        it.dubbing == request.dubbing
            }
            ?: firstOrNull { request.episode.isNotBlank() && it.episode == request.episode }

    private fun List<PlayerSourceVideo>.toPlayerSourceGraph(
        selectedVideo: PlayerSourceVideo,
        screenshotByEpisode: Map<String, String>,
        fallbackScreenshotUrl: String,
    ): PlayerSourceGraph {
        val videos = if (isEmpty()) listOf(selectedVideo) else this
        val supportedBalancers = videos
            .map { it.player }
            .distinct()
            .filter { player ->
                videos.firstOrNull { it.player == player }?.iframeUrl?.isSupportedPlayerUrl() == true
            }
        val balancerNames = (listOf(selectedVideo.player) + supportedBalancers)
            .distinct()
            .sortedBy { balancerName ->
                minOf(
                    balancerName.playerDisplayOrderPriority(),
                    videos.firstOrNull { it.player == balancerName }
                        ?.iframeUrl
                        ?.playerDisplayOrderPriority()
                        ?: OTHER_PLAYER_PRIORITY,
                )
            }
        val kodikIframeByEpisode = videos
            .filter { it.iframeUrl.isKodikPlayerUrl() }
            .groupBy { it.episode }
            .mapValues { (_, group) -> group.first().iframeUrl }

        val balancers = balancerNames.map { balancerName ->
            val balancerVideos = videos.filter { it.player == balancerName }
            val dubbingNames = balancerVideos.map { it.dubbing }.distinct()
            PlayerSourceBalancer(
                name = balancerName,
                dubbings = dubbingNames.map { dubbingName ->
                    val episodes = balancerVideos
                        .filter { it.dubbing == dubbingName }
                        .sortedByEpisode()
                        .map { source ->
                            PlayerSourceEpisode(
                                id = source.id,
                                playerId = source.playerId,
                                number = source.episode,
                                iframeUrl = source.iframeUrl,
                                screenshotUrl = kodikIframeByEpisode[source.episode]
                                    ?: screenshotByEpisode[source.episode]
                                    ?: fallbackScreenshotUrl.takeIf { source.id == selectedVideo.id }
                                    ?: "",
                                skips = source.skips,
                            )
                        }
                    PlayerSourceDubbing(
                        name = dubbingName,
                        episodes = episodes,
                        views = balancerVideos.filter { it.dubbing == dubbingName }.sumViews(),
                    )
                },
            )
        }

        val balancerIndex =
            balancers.indexOfFirst { it.name == selectedVideo.player }.coerceAtLeast(0)
        val selectedBalancer = balancers.getOrNull(balancerIndex)
        val dubbingIndex = selectedBalancer
            ?.dubbings
            ?.indexOfFirst { it.name == selectedVideo.dubbing }
            ?.coerceAtLeast(0)
            ?: 0
        val selectedDubbing = selectedBalancer?.dubbings?.getOrNull(dubbingIndex)
        val episodeIndex = selectedDubbing
            ?.episodes
            ?.indexOfFirst { episode ->
                episode.id == selectedVideo.id ||
                        (episode.number == selectedVideo.episode && episode.iframeUrl == selectedVideo.iframeUrl)
            }
            ?.coerceAtLeast(0)
            ?: 0

        return PlayerSourceGraph(
            balancers = balancers,
            selection = PlayerSourceSelection(
                balancerIndex = balancerIndex,
                dubbingIndex = dubbingIndex,
                episodeIndex = episodeIndex,
            ),
        )
    }

    private fun List<PlayerSourceVideo>.sortedByEpisode(): List<PlayerSourceVideo> =
        sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }

    private fun List<PlayerSourceVideo>.sumViews(): Int = sumOf { it.views ?: 0 }

    private companion object {
        const val OTHER_PLAYER_PRIORITY = 2
    }
}

package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.domain.player.model.PlayerSourceRequest
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import javax.inject.Inject

internal class PlayerDestinationStateMapper @Inject constructor() {
    fun toState(
        dest: PlayerDestination,
        autoSkipOpeningsEndings: Boolean = false,
    ): PlayerState.State {
        val sourceGraph = dest.toSingleEpisodeSourceGraph()
        val selection = sourceGraph.selection.normalizedFor(sourceGraph)
        return PlayerState.State(
            animeTitle = dest.animeTitle,
            animeId = dest.animeId,
            posterUrl = dest.posterUrl,
            sourceGraph = sourceGraph,
            sourceSelection = selection,
            autoSkipOpeningsEndings = autoSkipOpeningsEndings,
        )
    }

    fun toSourceRequest(dest: PlayerDestination): PlayerSourceRequest =
        PlayerSourceRequest(
            animeId = dest.animeId,
            iframeUrl = dest.iframeUrl,
            animeTitle = dest.animeTitle,
            episode = dest.episode,
            playerName = dest.playerName,
            dubbing = dest.dubbing,
            selectedVideoId = dest.selectedVideoId,
            selectedPlayerId = dest.selectedPlayerId,
            selectedScreenshotUrl = dest.selectedScreenshotUrl,
        )

    private fun PlayerDestination.toSingleEpisodeSourceGraph(): PlayerSourceGraph =
        PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer(
                    name = playerName,
                    dubbings = listOf(
                        PlayerSourceDubbing(
                            name = dubbing,
                            episodes = listOf(
                                PlayerSourceEpisode(
                                    id = selectedVideoId,
                                    playerId = selectedPlayerId,
                                    number = episode,
                                    iframeUrl = iframeUrl,
                                    screenshotUrl = selectedScreenshotUrl,
                                )
                            ),
                        )
                    ),
                )
            ),
            selection = PlayerSourceSelection(),
        )

    private fun PlayerSourceSelection.normalizedFor(graph: PlayerSourceGraph): PlayerSourceSelection {
        val balancer = balancerIndex.coerceIn(0, (graph.balancers.size - 1).coerceAtLeast(0))
        val dubbings = graph.balancers.getOrNull(balancer)?.dubbings.orEmpty()
        val dubbing = dubbingIndex.coerceIn(0, (dubbings.size - 1).coerceAtLeast(0))
        val episodes = dubbings.getOrNull(dubbing)?.episodes.orEmpty()
        val episode = episodeIndex.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
        return PlayerSourceSelection(balancer, dubbing, episode)
    }
}

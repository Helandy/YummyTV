package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import javax.inject.Inject

@Suppress("DEPRECATION")
internal class PlayerDestinationStateMapper @Inject constructor() {
    fun toState(
        dest: PlayerDestination,
        autoSkipOpeningsEndings: Boolean = false,
    ): PlayerState.State {
        val sourceGraph = dest.sourceGraph.takeIf { it.balancers.isNotEmpty() }
            ?: dest.toLegacySourceGraph()
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

    private fun PlayerDestination.toLegacySourceGraph(): PlayerSourceGraph {
        val balancers = if (allBalancerNames.isNotEmpty()) {
            allBalancerNames.mapIndexed { balancerIndex, balancerName ->
                val dubbingNames = allBalancerDubbingNames.getOrElse(balancerIndex) { emptyList() }
                    .ifEmpty { if (dubbing.isNotBlank()) listOf(dubbing) else emptyList() }
                PlayerSourceBalancer(
                    name = balancerName,
                    dubbings = dubbingNames.mapIndexed { dubbingIndex, dubbingName ->
                        PlayerSourceDubbing(
                            name = dubbingName,
                            episodes = legacyEpisodesFor(balancerIndex, dubbingIndex),
                            views = allBalancerDubbingViews
                                .getOrElse(balancerIndex) { emptyList() }
                                .getOrElse(dubbingIndex) { 0 },
                        )
                    },
                )
            }
        } else {
            val dubbingNames = allDubbingNames.ifEmpty {
                if (dubbing.isNotBlank()) listOf(dubbing) else listOf("")
            }
            listOf(
                PlayerSourceBalancer(
                    name = playerName,
                    dubbings = dubbingNames.mapIndexed { index, dubbingName ->
                        PlayerSourceDubbing(
                            name = dubbingName,
                            episodes = legacyEpisodesFor(index),
                            views = allDubbingViews.getOrElse(index) { 0 },
                        )
                    },
                )
            )
        }
        val graph = PlayerSourceGraph(
            balancers = balancers,
            selection = PlayerSourceSelection(
                balancerIndex = currentBalancerIndex,
                dubbingIndex = currentDubbingIndex,
                episodeIndex = currentEpisodeIndex,
            ),
        )
        return graph.copy(selection = graph.selection.normalizedFor(graph))
    }

    private fun PlayerDestination.legacyEpisodesFor(
        balancerIndex: Int,
        dubbingIndex: Int,
    ): List<PlayerSourceEpisode> {
        val urls = allBalancerEpisodeUrls.getOrElse(balancerIndex) { emptyList() }
            .getOrElse(dubbingIndex) { emptyList() }
        val numbers = allBalancerEpisodeNumbers.getOrElse(balancerIndex) { emptyList() }
            .getOrElse(dubbingIndex) { emptyList() }
        val ids = allBalancerEpisodeVideoIds.getOrElse(balancerIndex) { emptyList() }
            .getOrElse(dubbingIndex) { emptyList() }
        val skips = allBalancerEpisodeSkips.getOrElse(balancerIndex) { emptyList() }
            .getOrElse(dubbingIndex) { emptyList() }
        val screenshots = if (
            balancerIndex == currentBalancerIndex &&
            dubbingIndex == currentDubbingIndex
        ) {
            screenshotUrls
        } else {
            emptyList()
        }
        return buildLegacyEpisodes(
            urls = urls,
            numbers = numbers,
            ids = ids,
            skips = skips,
            screenshots = screenshots,
        )
    }

    private fun PlayerDestination.legacyEpisodesFor(dubbingIndex: Int): List<PlayerSourceEpisode> {
        val urls = allDubbingEpisodeUrls.getOrElse(dubbingIndex) { emptyList() }
            .ifEmpty { if (dubbingIndex == currentDubbingIndex) episodeUrls else emptyList() }
        val numbers = allDubbingEpisodeNumbers.getOrElse(dubbingIndex) { emptyList() }
            .ifEmpty { if (dubbingIndex == currentDubbingIndex) episodeNumbers else emptyList() }
        val ids = allDubbingEpisodeVideoIds.getOrElse(dubbingIndex) { emptyList() }
            .ifEmpty { if (dubbingIndex == currentDubbingIndex) episodeVideoIds else emptyList() }
        val skips = allDubbingEpisodeSkips.getOrElse(dubbingIndex) { emptyList() }
            .ifEmpty { if (dubbingIndex == currentDubbingIndex) episodeSkips else emptyList() }
        val screenshots = if (dubbingIndex == currentDubbingIndex) screenshotUrls else emptyList()
        return buildLegacyEpisodes(
            urls = urls,
            numbers = numbers,
            ids = ids,
            skips = skips,
            screenshots = screenshots,
        )
    }

    private fun PlayerDestination.buildLegacyEpisodes(
        urls: List<String>,
        numbers: List<String>,
        ids: List<Int>,
        skips: List<PlayerSkips>,
        screenshots: List<String>,
    ): List<PlayerSourceEpisode> {
        val size = maxOf(urls.size, numbers.size, ids.size, skips.size, screenshots.size)
        if (size == 0) {
            return listOf(
                PlayerSourceEpisode(
                    id = episodeVideoIds.getOrElse(currentEpisodeIndex) { 0 },
                    number = episode,
                    iframeUrl = iframeUrl,
                    screenshotUrl = screenshotUrls.getOrElse(currentEpisodeIndex) { "" },
                    skips = episodeSkips.getOrElse(currentEpisodeIndex) { PlayerSkips.Empty },
                )
            )
        }
        return List(size) { index ->
            PlayerSourceEpisode(
                id = ids.getOrElse(index) { 0 },
                number = numbers.getOrElse(index) {
                    episode.takeIf { index == currentEpisodeIndex }.orEmpty()
                },
                iframeUrl = urls.getOrElse(index) {
                    iframeUrl.takeIf { index == currentEpisodeIndex }.orEmpty()
                },
                screenshotUrl = screenshots.getOrElse(index) { "" },
                skips = skips.getOrElse(index) { PlayerSkips.Empty },
            )
        }
    }

    private fun PlayerSourceSelection.normalizedFor(graph: PlayerSourceGraph): PlayerSourceSelection {
        val balancer = balancerIndex.coerceIn(0, (graph.balancers.size - 1).coerceAtLeast(0))
        val dubbings = graph.balancers.getOrNull(balancer)?.dubbings.orEmpty()
        val dubbing = dubbingIndex.coerceIn(0, (dubbings.size - 1).coerceAtLeast(0))
        val episodes = dubbings.getOrNull(dubbing)?.episodes.orEmpty()
        val episode = episodeIndex.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
        return PlayerSourceSelection(balancer, dubbing, episode)
    }
}

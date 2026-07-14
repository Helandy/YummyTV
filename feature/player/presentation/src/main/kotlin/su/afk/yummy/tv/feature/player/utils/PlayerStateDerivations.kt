package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState

internal fun normalizedSourceSelection(state: PlayerState.State): PlayerSourceSelection =
    state.sourceSelection.normalizedFor(state.sourceGraph.balancers)

internal fun PlayerSourceSelection.normalizedFor(
    balancers: List<PlayerSourceBalancer>,
): PlayerSourceSelection {
    val balancer = balancerIndex.coerceIn(0, (balancers.size - 1).coerceAtLeast(0))
    val dubbings = balancers.getOrNull(balancer)?.dubbings.orEmpty()
    val dubbing = dubbingIndex.coerceIn(0, (dubbings.size - 1).coerceAtLeast(0))
    val episodes = dubbings.getOrNull(dubbing)?.episodes.orEmpty()
    val episode = episodeIndex.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
    return PlayerSourceSelection(
        balancerIndex = balancer,
        dubbingIndex = dubbing,
        episodeIndex = episode,
    )
}

internal fun activeBalancer(state: PlayerState.State): PlayerSourceBalancer? {
    val selection = normalizedSourceSelection(state)
    return state.sourceGraph.balancers.getOrNull(selection.balancerIndex)
}

internal fun activeDubbing(state: PlayerState.State): PlayerSourceDubbing? {
    val selection = normalizedSourceSelection(state)
    return activeBalancer(state)?.dubbings?.getOrNull(selection.dubbingIndex)
}

internal fun activeEpisodeSource(state: PlayerState.State): PlayerSourceEpisode? {
    val selection = normalizedSourceSelection(state)
    return activeDubbing(state)?.episodes?.getOrNull(selection.episodeIndex)
}

internal fun activeDubbingEpisodes(state: PlayerState.State): List<PlayerSourceEpisode> =
    activeDubbing(state)?.episodes.orEmpty()

internal fun activeIframeUrl(state: PlayerState.State): String =
    activeEpisodeSource(state)?.iframeUrl.orEmpty()

internal fun activeEpisode(state: PlayerState.State): String =
    activeEpisodeSource(state)?.number.orEmpty()

internal fun activeVideoId(state: PlayerState.State): Int =
    activeEpisodeSource(state)?.id ?: 0

internal fun activePlayerId(state: PlayerState.State): Int? =
    activeEpisodeSource(state)?.playerId

internal fun activeScreenshotUrl(state: PlayerState.State): String =
    activeEpisodeSource(state)?.screenshotUrl.orEmpty()

internal fun activeBalancerName(state: PlayerState.State): String =
    activeBalancer(state)?.name.orEmpty()

internal fun activeDubbingName(state: PlayerState.State): String =
    activeDubbing(state)?.name.orEmpty()

internal fun globalDubbingNames(state: PlayerState.State): List<String> =
    state.sourceGraph.balancers
        .flatMap { balancer -> balancer.dubbings.map { it.name } }
        .distinct()
        .sortedWith(
            compareByDescending<String> { dubbingName ->
                globalDubbingViews(state, dubbingName)
            }.thenBy { it }
        )

internal fun globalDubbingEpisodeNumbers(
    state: PlayerState.State,
    dubbingName: String,
): List<String> =
    state.sourceGraph.balancers
        .flatMap { balancer ->
            balancer.dubbings
                .filter { it.name == dubbingName }
                .flatMap { dubbing -> dubbing.episodes.map { it.number } }
        }
        .distinct()

internal fun globalDubbingViews(
    state: PlayerState.State,
    dubbingName: String,
): Int =
    state.sourceGraph.balancers
        .mapNotNull { balancer -> balancer.dubbings.firstOrNull { it.name == dubbingName }?.views }
        .maxOrNull()
        ?: 0

internal fun globalDubbingSourceNames(
    state: PlayerState.State,
    dubbingName: String,
    playerNamePrefix: String,
): String =
    state.sourceGraph.balancers
        .filter { balancer -> balancer.dubbings.any { it.name == dubbingName } }
        .map { it.name.removePrefix(playerNamePrefix) }
        .joinToString(" • ")

internal fun availableBalancerIndices(
    state: PlayerState.State,
    dubbingName: String,
): List<Int> =
    state.sourceGraph.balancers.mapIndexedNotNull { index, balancer ->
        index.takeIf { balancer.dubbings.any { dubbing -> dubbing.name == dubbingName } }
    }

internal fun isBalancerAvailableForEpisode(
    state: PlayerState.State,
    balancerIndex: Int,
    dubbingName: String,
    episodeNumber: String,
): Boolean = state.sourceGraph.balancers
    .getOrNull(balancerIndex)
    ?.dubbings
    ?.firstOrNull { it.name == dubbingName }
    ?.episodes
    ?.any { it.number == episodeNumber }
    ?: false

internal fun isDubbingAvailableForEpisode(
    state: PlayerState.State,
    dubbingName: String,
    episodeNumber: String,
): Boolean = state.sourceGraph.balancers.any { balancer ->
    balancer.dubbings
        .firstOrNull { it.name == dubbingName }
        ?.episodes
        ?.any { it.number == episodeNumber }
        ?: false
}

internal fun resolveDubbingSource(
    state: PlayerState.State,
    dubbingName: String,
    episodeNumber: String,
): DubbingSource? {
    val currentSelection = normalizedSourceSelection(state)
    val candidates = state.sourceGraph.balancers.flatMapIndexed { balancerIndex, balancer ->
        balancer.dubbings.mapIndexedNotNull { dubbingIndex, dubbing ->
            if (dubbing.name != dubbingName) return@mapIndexedNotNull null
            val episodeIndex = dubbing.episodes
                .indexOfFirst { it.number == episodeNumber }
                .takeIf { it >= 0 }
                ?: return@mapIndexedNotNull null
            DubbingSource(
                balancerIndex = balancerIndex,
                dubbingIndex = dubbingIndex,
                episodeIndex = episodeIndex,
            )
        }
    }

    return candidates.firstOrNull { it.balancerIndex == currentSelection.balancerIndex }
        ?: candidates.firstOrNull()
}

internal fun isFinalAvailableEpisode(
    state: PlayerState.State,
    activeEpisode: String,
): Boolean {
    val currentNumber = activeEpisode.toIntOrNull()
    val activeIndex = normalizedSourceSelection(state).episodeIndex
    val activeSize = activeDubbingEpisodes(state).size
    if (currentNumber == null) return activeIndex >= activeSize - 1
    val maxNumber = state.sourceGraph.balancers
        .flatMap { balancer -> balancer.dubbings }
        .flatMap { dubbing -> dubbing.episodes }
        .mapNotNull { it.number.toIntOrNull() }
        .maxOrNull()
    return maxNumber == null || currentNumber >= maxNumber
}

internal data class DubbingSource(
    val balancerIndex: Int,
    val dubbingIndex: Int,
    val episodeIndex: Int,
)

internal data class PlayerResizeSettingsScope(
    val animeId: Int,
    val animeTitle: String,
    val playerName: String,
)

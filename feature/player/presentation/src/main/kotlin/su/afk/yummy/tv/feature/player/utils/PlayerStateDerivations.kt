package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerState

internal fun activeAllDubbingNames(state: PlayerState.State): List<String> =
    if (state.allBalancerDubbingNames.isNotEmpty()) {
        state.allBalancerDubbingNames.getOrElse(state.balancerIndex) { state.allDubbingNames }
    } else {
        state.allDubbingNames
    }

internal fun activeAllEpisodeUrls(state: PlayerState.State): List<List<String>> =
    if (state.allBalancerEpisodeUrls.isNotEmpty()) {
        state.allBalancerEpisodeUrls.getOrElse(state.balancerIndex) { state.allDubbingEpisodeUrls }
    } else {
        state.allDubbingEpisodeUrls
    }

internal fun activeAllEpisodeNumbers(state: PlayerState.State): List<List<String>> =
    if (state.allBalancerEpisodeNumbers.isNotEmpty()) {
        state.allBalancerEpisodeNumbers.getOrElse(state.balancerIndex) { state.allDubbingEpisodeNumbers }
    } else {
        state.allDubbingEpisodeNumbers
    }

internal fun activeDubbingUrls(state: PlayerState.State): List<String> =
    activeAllEpisodeUrls(state).getOrElse(state.dubbingIndex) { state.episodeUrls }

internal fun activeEpisodeNumbers(state: PlayerState.State): List<String> =
    activeAllEpisodeNumbers(state).getOrElse(state.dubbingIndex) { state.episodeNumbers }

internal fun activeIframeUrl(state: PlayerState.State): String =
    activeDubbingUrls(state).getOrElse(state.episodeIndex) { state.iframeUrl }

internal fun activeEpisode(state: PlayerState.State): String =
    activeEpisodeNumbers(state).getOrElse(state.episodeIndex) { state.episode }

internal fun activeDubbing(state: PlayerState.State): String =
    activeAllDubbingNames(state).getOrElse(state.dubbingIndex) { state.dubbing }

internal fun activeBalancerName(state: PlayerState.State): String =
    if (state.allBalancerNames.isNotEmpty()) {
        state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName }
    } else {
        state.playerName
    }

internal fun globalDubbingNames(state: PlayerState.State): List<String> =
    if (state.allBalancerDubbingNames.isNotEmpty()) {
        state.allBalancerDubbingNames.flatten().distinct()
    } else {
        state.allDubbingNames
    }

internal fun resolveDubbingSource(
    state: PlayerState.State,
    dubbingName: String,
    episodeNumber: String,
): DubbingSource? {
    if (state.allBalancerDubbingNames.isEmpty()) {
        val dubbingIndex =
            state.allDubbingNames.indexOf(dubbingName).takeIf { it >= 0 } ?: return null
        val episodeNumbers = state.allDubbingEpisodeNumbers.getOrElse(dubbingIndex) { emptyList() }
        val episodeIndex = episodeNumbers.indexOf(episodeNumber).takeIf { it >= 0 } ?: 0
        return DubbingSource(
            balancerIndex = state.balancerIndex,
            dubbingIndex = dubbingIndex,
            episodeIndex = episodeIndex,
        )
    }

    val candidates = state.allBalancerDubbingNames.flatMapIndexed { balancerIndex, dubbingNames ->
        dubbingNames.mapIndexedNotNull { dubbingIndex, name ->
            if (name != dubbingName) return@mapIndexedNotNull null
            val episodeNumbers = state.allBalancerEpisodeNumbers
                .getOrElse(balancerIndex) { emptyList() }
                .getOrElse(dubbingIndex) { emptyList() }
            val episodeIndex = episodeNumbers.indexOf(episodeNumber).takeIf { it >= 0 } ?: 0
            DubbingSource(
                balancerIndex = balancerIndex,
                dubbingIndex = dubbingIndex,
                episodeIndex = episodeIndex,
            )
        }
    }

    return candidates.firstOrNull { it.balancerIndex == state.balancerIndex }
        ?: candidates.firstOrNull()
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

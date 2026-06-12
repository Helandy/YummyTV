package su.afk.yummy.tv.feature.player.model

import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState

internal data class MobilePlayerUiState(
    val activeIframeUrl: String,
    val activeEpisode: String,
    val activeVideoId: Int,
    val activeDubbing: String,
    val activeBalancerName: String,
    val activeScreenshotUrl: String,
    val activeSkips: PlayerSkips,
    val hasPrevEpisode: Boolean,
    val hasNextEpisode: Boolean,
    val dubbingNames: List<String>,
    val dubbingEpisodeCounts: List<Int>,
    val dubbingViews: List<Int>,
    val dubbingSourceNames: List<String>,
    val currentDubbingIndex: Int,
    val balancerNames: List<String>,
    val availableBalancerIndices: List<Int>,
    val currentBalancerIndex: Int,
) {
    companion object {
        fun from(
            state: PlayerState.State,
            playerNamePrefix: String,
        ): MobilePlayerUiState {
            val allDubbingNames = if (state.allBalancerDubbingNames.isNotEmpty()) {
                state.allBalancerDubbingNames.getOrElse(state.balancerIndex) { state.allDubbingNames }
            } else {
                state.allDubbingNames
            }
            val allEpisodeUrls = if (state.allBalancerEpisodeUrls.isNotEmpty()) {
                state.allBalancerEpisodeUrls.getOrElse(state.balancerIndex) { state.allDubbingEpisodeUrls }
            } else {
                state.allDubbingEpisodeUrls
            }
            val allEpisodeNumbers = if (state.allBalancerEpisodeNumbers.isNotEmpty()) {
                state.allBalancerEpisodeNumbers.getOrElse(state.balancerIndex) { state.allDubbingEpisodeNumbers }
            } else {
                state.allDubbingEpisodeNumbers
            }
            val allEpisodeVideoIds = if (state.allBalancerEpisodeVideoIds.isNotEmpty()) {
                state.allBalancerEpisodeVideoIds.getOrElse(state.balancerIndex) { state.allDubbingEpisodeVideoIds }
            } else {
                state.allDubbingEpisodeVideoIds
            }
            val allEpisodeSkips = if (state.allBalancerEpisodeSkips.isNotEmpty()) {
                state.allBalancerEpisodeSkips.getOrElse(state.balancerIndex) { state.allDubbingEpisodeSkips }
            } else {
                state.allDubbingEpisodeSkips
            }
            val allDubbingViews = if (state.allBalancerDubbingViews.isNotEmpty()) {
                state.allBalancerDubbingViews.getOrElse(state.balancerIndex) { state.allDubbingViews }
            } else {
                state.allDubbingViews
            }
            val activeUrls = allEpisodeUrls.getOrElse(state.dubbingIndex) { state.episodeUrls }
            val activeNumbers = allEpisodeNumbers.getOrElse(state.dubbingIndex) { state.episodeNumbers }
            val activeVideoIds = allEpisodeVideoIds.getOrElse(state.dubbingIndex) { state.episodeVideoIds }
            val activeSkips = allEpisodeSkips.getOrElse(state.dubbingIndex) { state.episodeSkips }
            val activeDubbing = allDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing }
            val globalDubbingNames = state.globalDubbingNames()
            val globalDubbingEpisodeNumbers =
                globalDubbingNames.map { state.globalDubbingEpisodeNumbers(it) }
            val globalDubbingViews = globalDubbingNames.map { state.globalDubbingViews(it) }
            val globalDubbingSourceNames = globalDubbingNames.map {
                state.globalDubbingSourceNames(
                    dubbingName = it,
                    playerNamePrefix = playerNamePrefix,
                )
            }
            val displayedDubbingNames = globalDubbingNames.ifEmpty { allDubbingNames }
            val displayedDubbingEpisodeNumbers =
                globalDubbingEpisodeNumbers.ifEmpty { allEpisodeNumbers }
            val displayedDubbingViews = globalDubbingViews.ifEmpty { allDubbingViews }
            val availableBalancerIndices = if (state.allBalancerDubbingNames.isEmpty()) {
                state.allBalancerNames.indices.toList()
            } else {
                state.allBalancerDubbingNames.mapIndexedNotNull { index, dubbingNames ->
                    index.takeIf { activeDubbing in dubbingNames }
                }
            }
            val balancerNames = availableBalancerIndices.map { index ->
                state.allBalancerNames.getOrElse(index) { state.playerName }
            }
            return MobilePlayerUiState(
                activeIframeUrl = activeUrls.getOrElse(state.episodeIndex) { state.iframeUrl },
                activeEpisode = activeNumbers.getOrElse(state.episodeIndex) { state.episode },
                activeVideoId = activeVideoIds.getOrElse(state.episodeIndex) { 0 },
                activeDubbing = activeDubbing,
                activeBalancerName = state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName },
                activeScreenshotUrl = state.screenshotUrls.getOrElse(state.episodeIndex) { "" },
                activeSkips = activeSkips.getOrElse(state.episodeIndex) { PlayerSkips.Empty },
                hasPrevEpisode = state.episodeIndex > 0,
                hasNextEpisode = state.episodeIndex < activeUrls.lastIndex,
                dubbingNames = displayedDubbingNames,
                dubbingEpisodeCounts = displayedDubbingNames.mapIndexed { index, _ ->
                    displayedDubbingEpisodeNumbers.getOrElse(index) { emptyList() }.distinct().size
                },
                dubbingViews = displayedDubbingNames.mapIndexed { index, _ ->
                    displayedDubbingViews.getOrElse(index) { 0 }
                },
                dubbingSourceNames = displayedDubbingNames.mapIndexed { index, _ ->
                    globalDubbingSourceNames.getOrElse(index) { "" }
                },
                currentDubbingIndex = displayedDubbingNames.indexOf(activeDubbing)
                    .takeIf { it >= 0 }
                    ?: state.dubbingIndex,
                balancerNames = balancerNames,
                availableBalancerIndices = availableBalancerIndices,
                currentBalancerIndex = availableBalancerIndices.indexOf(state.balancerIndex)
                    .takeIf { it >= 0 } ?: 0,
            )
        }
    }
}

private fun PlayerState.State.globalDubbingNames(): List<String> =
    if (allBalancerDubbingNames.isNotEmpty()) {
        allBalancerDubbingNames.flatten().distinct()
    } else {
        allDubbingNames
    }

private fun PlayerState.State.globalDubbingEpisodeNumbers(dubbingName: String): List<String> {
    if (allBalancerDubbingNames.isEmpty()) {
        val index = allDubbingNames.indexOf(dubbingName).takeIf { it >= 0 } ?: return emptyList()
        return allDubbingEpisodeNumbers.getOrElse(index) { emptyList() }
    }
    return allBalancerDubbingNames.flatMapIndexed { balancerIndex, dubbingNames ->
        val dubbingIndex = dubbingNames.indexOf(dubbingName)
        if (dubbingIndex < 0) {
            emptyList()
        } else {
            allBalancerEpisodeNumbers
                .getOrElse(balancerIndex) { emptyList() }
                .getOrElse(dubbingIndex) { emptyList() }
        }
    }.distinct()
}

private fun PlayerState.State.globalDubbingViews(dubbingName: String): Int {
    if (allBalancerDubbingNames.isEmpty()) {
        val index = allDubbingNames.indexOf(dubbingName).takeIf { it >= 0 } ?: return 0
        return allDubbingViews.getOrElse(index) { 0 }
    }
    return allBalancerDubbingNames.mapIndexedNotNull { balancerIndex, dubbingNames ->
        val dubbingIndex = dubbingNames.indexOf(dubbingName)
        if (dubbingIndex < 0) {
            null
        } else {
            allBalancerDubbingViews
                .getOrElse(balancerIndex) { emptyList() }
                .getOrElse(dubbingIndex) { 0 }
        }
    }.maxOrNull() ?: 0
}

private fun PlayerState.State.globalDubbingSourceNames(
    dubbingName: String,
    playerNamePrefix: String,
): String {
    if (allBalancerDubbingNames.isEmpty()) return playerName.removePrefix(playerNamePrefix)
    return allBalancerDubbingNames.mapIndexedNotNull { index, dubbingNames ->
        allBalancerNames.getOrNull(index)
            ?.takeIf { dubbingName in dubbingNames }
            ?.removePrefix(playerNamePrefix)
    }.joinToString(" • ")
}

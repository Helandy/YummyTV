package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState

internal data class PlayerScreenUiState(
    val activeIframeUrl: String,
    val activeEpisode: String,
    val activeVideoId: Int,
    val activeSkips: PlayerSkips,
    val activeScreenshotUrl: String,
    val activeBalancerName: String,
    val activeDubbing: String,
    val hasPrevEpisode: Boolean,
    val hasNextEpisode: Boolean,
    val canRateTitleOnEnd: Boolean,
    val dubbingOptions: DubbingOptionsUi,
    val currentDubbingIndex: Int,
    val availableBalancerIndices: List<Int>,
    val availableBalancerNames: List<String>,
    val currentBalancerIndex: Int,
)

internal data class DubbingOptionsUi(
    val names: List<String>,
    val episodeCounts: List<Int>,
    val views: List<Int>,
    val sourceNames: List<String>,
)

private data class DubbingOptionUi(
    val name: String,
    val views: Int,
    val episodeCount: Int,
    val sourceNames: String,
)

@Composable
internal fun rememberPlayerScreenUiState(
    state: PlayerState.State,
    playerNamePrefix: String,
): PlayerScreenUiState {
    val activeAllDubbingNames = if (state.allBalancerDubbingNames.isNotEmpty())
        state.allBalancerDubbingNames.getOrElse(state.balancerIndex) { state.allDubbingNames }
    else state.allDubbingNames

    val activeAllEpisodeUrls = if (state.allBalancerEpisodeUrls.isNotEmpty())
        state.allBalancerEpisodeUrls.getOrElse(state.balancerIndex) { state.allDubbingEpisodeUrls }
    else state.allDubbingEpisodeUrls

    val activeAllEpisodeNumbers = if (state.allBalancerEpisodeNumbers.isNotEmpty())
        state.allBalancerEpisodeNumbers.getOrElse(state.balancerIndex) { state.allDubbingEpisodeNumbers }
    else state.allDubbingEpisodeNumbers

    val activeAllEpisodeVideoIds = if (state.allBalancerEpisodeVideoIds.isNotEmpty())
        state.allBalancerEpisodeVideoIds.getOrElse(state.balancerIndex) { state.allDubbingEpisodeVideoIds }
    else state.allDubbingEpisodeVideoIds

    val activeAllDubbingViews = if (state.allBalancerDubbingViews.isNotEmpty())
        state.allBalancerDubbingViews.getOrElse(state.balancerIndex) { state.allDubbingViews }
    else state.allDubbingViews

    val activeAllEpisodeSkips = if (state.allBalancerEpisodeSkips.isNotEmpty())
        state.allBalancerEpisodeSkips.getOrElse(state.balancerIndex) { state.allDubbingEpisodeSkips }
    else state.allDubbingEpisodeSkips

    val activeDubbingUrls = activeAllEpisodeUrls.getOrElse(state.dubbingIndex) { state.episodeUrls }
    val activeEpisodeNumbers = activeAllEpisodeNumbers.getOrElse(state.dubbingIndex) { state.episodeNumbers }
    val activeEpisodeVideoIds = activeAllEpisodeVideoIds.getOrElse(state.dubbingIndex) { state.episodeVideoIds }
    val activeEpisodeSkips = activeAllEpisodeSkips.getOrElse(state.dubbingIndex) { state.episodeSkips }
    val activeDubbing = activeAllDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing }

    val globalDubbingNames = state.globalDubbingNames()
    val globalDubbingEpisodeNumbers = globalDubbingNames.map { state.globalDubbingEpisodeNumbers(it) }
    val globalDubbingViews = globalDubbingNames.map { state.globalDubbingViews(it) }
    val globalDubbingSourceNames = globalDubbingNames.map { state.globalDubbingSourceNames(it, playerNamePrefix) }
    val dubbingOptions = remember(
        globalDubbingNames,
        activeAllDubbingNames,
        globalDubbingEpisodeNumbers,
        activeAllEpisodeNumbers,
        globalDubbingViews,
        activeAllDubbingViews,
        globalDubbingSourceNames,
    ) {
        val names = globalDubbingNames.ifEmpty { activeAllDubbingNames }
        val episodeNumbers = globalDubbingEpisodeNumbers.ifEmpty { activeAllEpisodeNumbers }
        val views = globalDubbingViews.ifEmpty { activeAllDubbingViews }

        val options = names.mapIndexed { index, name ->
            DubbingOptionUi(
                name = name,
                views = views.getOrElse(index) { 0 },
                episodeCount = episodeNumbers.getOrElse(index) { emptyList() }.distinct().size,
                sourceNames = globalDubbingSourceNames.getOrElse(index) { "" },
            )
        }
        DubbingOptionsUi(
            names = options.map { it.name },
            episodeCounts = options.map { it.episodeCount },
            views = options.map { it.views },
            sourceNames = options.map { it.sourceNames },
        )
    }

    val activeEpisode = activeEpisodeNumbers.getOrElse(state.episodeIndex) { state.episode }
    val availableBalancerIndices = state.availableBalancerIndices(activeDubbing)
    val availableBalancerNames = availableBalancerIndices.map { index ->
        state.allBalancerNames.getOrElse(index) { state.playerName }
    }

    return PlayerScreenUiState(
        activeIframeUrl = activeDubbingUrls.getOrElse(state.episodeIndex) { state.iframeUrl },
        activeEpisode = activeEpisode,
        activeVideoId = activeEpisodeVideoIds.getOrElse(state.episodeIndex) { 0 },
        activeSkips = activeEpisodeSkips.getOrElse(state.episodeIndex) { PlayerSkips.Empty },
        activeScreenshotUrl = state.screenshotUrls.getOrElse(state.episodeIndex) { "" },
        activeBalancerName = if (state.allBalancerNames.isNotEmpty())
            state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName }
        else state.playerName,
        activeDubbing = activeDubbing,
        hasPrevEpisode = state.episodeIndex > 0,
        hasNextEpisode = state.episodeIndex < activeDubbingUrls.size - 1,
        canRateTitleOnEnd = state.isFinalAvailableEpisode(activeEpisode, activeDubbingUrls.size) && state.animeId > 0,
        dubbingOptions = dubbingOptions,
        currentDubbingIndex = globalDubbingNames.indexOf(activeDubbing).takeIf { it >= 0 } ?: state.dubbingIndex,
        availableBalancerIndices = availableBalancerIndices,
        availableBalancerNames = availableBalancerNames,
        currentBalancerIndex = availableBalancerIndices.indexOf(state.balancerIndex).takeIf { it >= 0 } ?: 0,
    )
}

private fun PlayerState.State.isFinalAvailableEpisode(activeEpisode: String, activeDubbingSize: Int): Boolean {
    val currentNumber = activeEpisode.toIntOrNull()
    if (currentNumber == null) return episodeIndex >= activeDubbingSize - 1
    val maxNumber = buildList {
        addAll(episodeNumbers.mapNotNull { it.toIntOrNull() })
        allDubbingEpisodeNumbers.forEach { episodes -> addAll(episodes.mapNotNull { it.toIntOrNull() }) }
        allBalancerEpisodeNumbers.forEach { balancer ->
            balancer.forEach { episodes -> addAll(episodes.mapNotNull { it.toIntOrNull() }) }
        }
    }.maxOrNull()
    return maxNumber == null || currentNumber >= maxNumber
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

private fun PlayerState.State.availableBalancerIndices(dubbingName: String): List<Int> =
    if (allBalancerDubbingNames.isEmpty()) {
        allBalancerNames.indices.toList()
    } else {
        allBalancerDubbingNames.mapIndexedNotNull { index, dubbingNames ->
            index.takeIf { dubbingName in dubbingNames }
        }
    }

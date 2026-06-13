package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import su.afk.yummy.tv.feature.player.utils.activeAllDubbingNames
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbing
import su.afk.yummy.tv.feature.player.utils.activeDubbingUrls
import su.afk.yummy.tv.feature.player.utils.activeEpisodeNumbers
import su.afk.yummy.tv.feature.player.utils.globalDubbingNames
import su.afk.yummy.tv.feature.player.utils.resolveDubbingSource
import javax.inject.Inject

internal class PlayerSourceSelectionHandler @Inject constructor() {
    fun previousEpisode(state: PlayerState.State): PlayerState.State? =
        state.episodeIndex
            .takeIf { it > 0 }
            ?.let { state.copy(episodeIndex = it - 1) }

    fun nextEpisode(state: PlayerState.State): PlayerState.State? {
        val urls = activeDubbingUrls(state)
        return if (state.episodeIndex < urls.size - 1) {
            state.copy(episodeIndex = state.episodeIndex + 1)
        } else {
            null
        }
    }

    fun selectDubbing(
        state: PlayerState.State,
        index: Int,
        currentPosMs: Long,
    ): PlayerState.State? {
        val currentNum = activeEpisodeNumbers(state).getOrElse(state.episodeIndex) { "" }
        val selectedDubbing = globalDubbingNames(state).getOrElse(index) {
            activeAllDubbingNames(state).getOrElse(index) { "" }
        }
        if (selectedDubbing.isBlank()) return null
        val selection = resolveDubbingSource(
            state = state,
            dubbingName = selectedDubbing,
            episodeNumber = currentNum,
        ) ?: return null
        if (
            selection.balancerIndex == state.balancerIndex &&
            selection.dubbingIndex == state.dubbingIndex &&
            selection.episodeIndex == state.episodeIndex
        ) return null

        return state.copy(
            dubbingResumeMs = (currentPosMs - RESUME_BACKOFF_MS).coerceAtLeast(0L),
            balancerIndex = selection.balancerIndex,
            dubbingIndex = selection.dubbingIndex,
            episodeIndex = selection.episodeIndex,
        )
    }

    fun selectBalancer(
        state: PlayerState.State,
        index: Int,
        currentPosMs: Long,
    ): PlayerState.State? {
        if (index == state.balancerIndex) return null
        val newDubbingNames = state.allBalancerDubbingNames.getOrElse(index) { emptyList() }
        val currentDubbingName = activeDubbing(state)
        val newDubbingIdx = newDubbingNames.indexOf(currentDubbingName).takeIf { it >= 0 } ?: 0
        val currentEpNum =
            activeEpisodeNumbers(state).getOrElse(state.episodeIndex) { state.episode }
        val newEpNums = state.allBalancerEpisodeNumbers.getOrElse(index) { emptyList() }
            .getOrElse(newDubbingIdx) { emptyList() }
        val newEpisodeIdx = newEpNums.indexOf(currentEpNum).takeIf { it >= 0 } ?: 0

        return state.copy(
            dubbingResumeMs = (currentPosMs - RESUME_BACKOFF_MS).coerceAtLeast(0L),
            balancerIndex = index,
            dubbingIndex = newDubbingIdx,
            episodeIndex = newEpisodeIdx,
        )
    }

    fun resizeSettingsScope(state: PlayerState.State): PlayerResizeSettingsScope =
        PlayerResizeSettingsScope(
            animeId = state.animeId,
            animeTitle = state.animeTitle,
            playerName = activeBalancerName(state),
        )

    private companion object {
        const val RESUME_BACKOFF_MS = 3_000L
    }
}

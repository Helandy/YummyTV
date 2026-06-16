package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingEpisodes
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.globalDubbingNames
import su.afk.yummy.tv.feature.player.utils.normalizedSourceSelection
import su.afk.yummy.tv.feature.player.utils.resolveDubbingSource
import javax.inject.Inject

internal class PlayerSourceSelectionHandler @Inject constructor() {
    fun previousEpisode(state: PlayerState.State): PlayerState.State? {
        val selection = normalizedSourceSelection(state)
        return selection.episodeIndex
            .takeIf { it > 0 }
            ?.let {
                state.withEpisodeSelection(selection.copy(episodeIndex = it - 1))
            }
    }

    fun nextEpisode(state: PlayerState.State): PlayerState.State? {
        val selection = normalizedSourceSelection(state)
        val episodes = activeDubbingEpisodes(state)
        return if (selection.episodeIndex < episodes.size - 1) {
            state.withEpisodeSelection(selection.copy(episodeIndex = selection.episodeIndex + 1))
        } else {
            null
        }
    }

    fun selectDubbing(
        state: PlayerState.State,
        index: Int,
        currentPosMs: Long,
    ): PlayerState.State? {
        val currentSelection = normalizedSourceSelection(state)
        val currentNum = activeEpisode(state)
        val selectedDubbing = globalDubbingNames(state).getOrElse(index) { "" }
        if (selectedDubbing.isBlank()) return null
        val selection = resolveDubbingSource(
            state = state,
            dubbingName = selectedDubbing,
            episodeNumber = currentNum,
        ) ?: return null
        if (selection == currentSelection.toDubbingSource()) return null

        return state.copy(
            dubbingResumeMs = (currentPosMs - RESUME_BACKOFF_MS).coerceAtLeast(0L),
            sourceSelection = PlayerSourceSelection(
                balancerIndex = selection.balancerIndex,
                dubbingIndex = selection.dubbingIndex,
                episodeIndex = selection.episodeIndex,
            ),
        )
    }

    fun selectBalancer(
        state: PlayerState.State,
        index: Int,
        currentPosMs: Long,
    ): PlayerState.State? {
        val currentSelection = normalizedSourceSelection(state)
        if (index == currentSelection.balancerIndex) return null
        val balancer = state.sourceGraph.balancers.getOrNull(index) ?: return null
        val newDubbingNames = balancer.dubbings.map { it.name }
        val currentDubbingName = activeDubbingName(state)
        val newDubbingIdx = newDubbingNames.indexOf(currentDubbingName).takeIf { it >= 0 } ?: 0
        val currentEpNum = activeEpisode(state)
        val newEpNums = balancer.dubbings.getOrNull(newDubbingIdx)
            ?.episodes
            ?.map { it.number }
            .orEmpty()
        val newEpisodeIdx = newEpNums.indexOf(currentEpNum).takeIf { it >= 0 } ?: 0

        return state.copy(
            dubbingResumeMs = (currentPosMs - RESUME_BACKOFF_MS).coerceAtLeast(0L),
            sourceSelection = PlayerSourceSelection(
                balancerIndex = index,
                dubbingIndex = newDubbingIdx,
                episodeIndex = newEpisodeIdx,
            ),
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

private fun PlayerState.State.withEpisodeSelection(selection: PlayerSourceSelection): PlayerState.State =
    copy(
        sourceSelection = selection,
        dubbingResumeMs = -1L,
        resumeFromMs = 0L,
        playbackPositionMs = 0L,
        playbackDurationMs = 0L,
    )

private fun PlayerSourceSelection.toDubbingSource() =
    su.afk.yummy.tv.feature.player.utils.DubbingSource(
        balancerIndex = balancerIndex,
        dubbingIndex = dubbingIndex,
        episodeIndex = episodeIndex,
    )

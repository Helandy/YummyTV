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
    val currentDubbingIndex: Int,
    val balancerNames: List<String>,
    val currentBalancerIndex: Int,
) {
    companion object {
        fun from(state: PlayerState.State): MobilePlayerUiState {
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
            val activeUrls = allEpisodeUrls.getOrElse(state.dubbingIndex) { state.episodeUrls }
            val activeNumbers = allEpisodeNumbers.getOrElse(state.dubbingIndex) { state.episodeNumbers }
            val activeVideoIds = allEpisodeVideoIds.getOrElse(state.dubbingIndex) { state.episodeVideoIds }
            val activeSkips = allEpisodeSkips.getOrElse(state.dubbingIndex) { state.episodeSkips }
            return MobilePlayerUiState(
                activeIframeUrl = activeUrls.getOrElse(state.episodeIndex) { state.iframeUrl },
                activeEpisode = activeNumbers.getOrElse(state.episodeIndex) { state.episode },
                activeVideoId = activeVideoIds.getOrElse(state.episodeIndex) { 0 },
                activeDubbing = allDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing },
                activeBalancerName = state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName },
                activeScreenshotUrl = state.screenshotUrls.getOrElse(state.episodeIndex) { "" },
                activeSkips = activeSkips.getOrElse(state.episodeIndex) { PlayerSkips.Empty },
                hasPrevEpisode = state.episodeIndex > 0,
                hasNextEpisode = state.episodeIndex < activeUrls.lastIndex,
                dubbingNames = if (state.allBalancerDubbingNames.isNotEmpty()) {
                    state.allBalancerDubbingNames.flatten().distinct()
                } else {
                    state.allDubbingNames
                },
                currentDubbingIndex = if (state.allBalancerDubbingNames.isNotEmpty()) {
                    state.allBalancerDubbingNames.flatten()
                        .distinct()
                        .indexOf(allDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing })
                        .coerceAtLeast(0)
                } else {
                    state.dubbingIndex
                },
                balancerNames = state.allBalancerNames,
                currentBalancerIndex = state.balancerIndex,
            )
        }
    }
}

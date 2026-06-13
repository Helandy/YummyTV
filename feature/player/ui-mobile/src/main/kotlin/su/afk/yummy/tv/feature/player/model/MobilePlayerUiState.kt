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
            val playback = state.toPlayerPlaybackUiState(playerNamePrefix)
            return MobilePlayerUiState(
                activeIframeUrl = playback.activeIframeUrl,
                activeEpisode = playback.activeEpisode,
                activeVideoId = playback.activeVideoId,
                activeDubbing = playback.activeDubbing,
                activeBalancerName = playback.activeBalancerName,
                activeScreenshotUrl = playback.activeScreenshotUrl,
                activeSkips = playback.activeSkips,
                hasPrevEpisode = playback.hasPrevEpisode,
                hasNextEpisode = playback.hasNextEpisode,
                dubbingNames = playback.dubbingNames,
                dubbingEpisodeCounts = playback.dubbingEpisodeCounts,
                dubbingViews = playback.dubbingViews,
                dubbingSourceNames = playback.dubbingSourceNames,
                currentDubbingIndex = playback.currentDubbingIndex,
                balancerNames = playback.balancerNames,
                availableBalancerIndices = playback.availableBalancerIndices,
                currentBalancerIndex = playback.currentBalancerIndex,
            )
        }
    }
}

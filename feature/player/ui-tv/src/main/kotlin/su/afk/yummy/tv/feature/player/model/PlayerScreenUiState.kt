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

@Composable
internal fun rememberPlayerScreenUiState(
    state: PlayerState.State,
    playerNamePrefix: String,
): PlayerScreenUiState {
    val playback = remember(state, playerNamePrefix) {
        state.toPlayerPlaybackUiState(playerNamePrefix)
    }
    return PlayerScreenUiState(
        activeIframeUrl = playback.activeIframeUrl,
        activeEpisode = playback.activeEpisode,
        activeVideoId = playback.activeVideoId,
        activeSkips = playback.activeSkips,
        activeScreenshotUrl = playback.activeScreenshotUrl,
        activeBalancerName = playback.activeBalancerName,
        activeDubbing = playback.activeDubbing,
        hasPrevEpisode = playback.hasPrevEpisode,
        hasNextEpisode = playback.hasNextEpisode,
        canRateTitleOnEnd = playback.canRateTitleOnEnd,
        dubbingOptions = DubbingOptionsUi(
            names = playback.dubbingNames,
            episodeCounts = playback.dubbingEpisodeCounts,
            views = playback.dubbingViews,
            sourceNames = playback.dubbingSourceNames,
        ),
        currentDubbingIndex = playback.currentDubbingIndex,
        availableBalancerIndices = playback.availableBalancerIndices,
        availableBalancerNames = playback.balancerNames,
        currentBalancerIndex = playback.currentBalancerIndex,
    )
}

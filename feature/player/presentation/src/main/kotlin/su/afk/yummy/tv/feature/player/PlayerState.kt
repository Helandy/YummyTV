package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

class PlayerState {
    data class State(
        val iframeUrl: String = "",
        val animeTitle: String = "",
        val episode: String = "",
        val playerName: String = "",
        val dubbing: String = "",
        val episodeUrls: List<String> = emptyList(),
        val episodeNumbers: List<String> = emptyList(),
        val episodeVideoIds: List<Int> = emptyList(),
        val screenshotUrls: List<String> = emptyList(),
        val animeId: Int = 0,
        val posterUrl: String = "",
        val allDubbingNames: List<String> = emptyList(),
        val allDubbingEpisodeUrls: List<List<String>> = emptyList(),
        val allDubbingEpisodeNumbers: List<List<String>> = emptyList(),
        val allDubbingEpisodeVideoIds: List<List<Int>> = emptyList(),
        val allDubbingViews: List<Int> = emptyList(),
        val allBalancerNames: List<String> = emptyList(),
        val allBalancerDubbingNames: List<List<String>> = emptyList(),
        val allBalancerEpisodeUrls: List<List<List<String>>> = emptyList(),
        val allBalancerEpisodeNumbers: List<List<List<String>>> = emptyList(),
        val allBalancerEpisodeVideoIds: List<List<List<Int>>> = emptyList(),
        val allBalancerDubbingViews: List<List<Int>> = emptyList(),
        val episodeSkips: List<PlayerSkips> = emptyList(),
        val allDubbingEpisodeSkips: List<List<PlayerSkips>> = emptyList(),
        val allBalancerEpisodeSkips: List<List<List<PlayerSkips>>> = emptyList(),
        val balancerIndex: Int = 0,
        val dubbingIndex: Int = 0,
        val episodeIndex: Int = 0,
        val dubbingResumeMs: Long = -1L,
        val retryKey: Int = 0,
        val streamUrl: String? = null,
        val streamHeaders: Map<String, String> = emptyMap(),
        val streamQualityMap: LinkedHashMap<String, String>? = null,
        val playerError: String? = null,
        val kodikBlockedError: String? = null,
        val resumeFromMs: Long = 0L,
        val autoSkipOpeningsEndings: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Back : Event
        data object PrevEpisode : Event
        data object NextEpisode : Event
        data class DubbingSelected(val index: Int, val currentPosMs: Long) : Event
        data class BalancerSelected(val index: Int, val currentPosMs: Long) : Event
        data class SaveProgress(val posMs: Long, val durMs: Long) : Event
        data class PlaybackError(val message: String) : Event
        data object RetryStream : Event
        data object RateTitle : Event
    }

    sealed interface Effect : UiEffect
}

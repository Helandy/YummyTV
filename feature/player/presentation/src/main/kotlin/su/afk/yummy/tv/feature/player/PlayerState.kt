package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel

data class PlayerProgressSnapshot(
    val episode: String,
    val episodeUrl: String,
    val videoId: Int,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
    val positionMs: Long,
    val durationMs: Long,
)

enum class PlayerSkipType {
    Opening,
    Ending,
}

enum class PlayerNextEpisodeSource {
    Controls,
    EndPrompt,
}

class PlayerState {
    data class State(
        val animeTitle: String = "",
        val animeId: Int = 0,
        val posterUrl: String = "",
        val sourceGraph: PlayerSourceGraph = PlayerSourceGraph(),
        val sourceSelection: PlayerSourceSelection = PlayerSourceSelection(),
        val dubbingResumeMs: Long = -1L,
        val retryKey: Int = 0,
        val streamUrl: String? = null,
        val streamHeaders: Map<String, String> = emptyMap(),
        val streamQualityMap: LinkedHashMap<String, String>? = null,
        val selectedQuality: String? = null,
        val selectedSpeed: Float = 1f,
        val resizeMode: PlayerResizeMode = PlayerResizeMode.FIT,
        val zoomLevel: PlayerZoomLevel = PlayerZoomLevel.PERCENT_10,
        val playerError: String? = null,
        val kodikBlockedError: String? = null,
        val resumeFromMs: Long = 0L,
        val playbackPositionMs: Long = 0L,
        val playbackDurationMs: Long = 0L,
        val autoSkipOpeningsEndings: Boolean = false,
        val mobileVideoScale: Float = 1f,
        val mobileVideoOffsetX: Float = 0f,
        val mobileVideoOffsetY: Float = 0f,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Back : Event
        data object OpenDetails : Event
        data object PrevEpisode : Event
        data class NextEpisode(
            val source: PlayerNextEpisodeSource = PlayerNextEpisodeSource.Controls,
        ) : Event

        data class EpisodeCompleted(
            val positionMs: Long,
            val durationMs: Long,
            val episodeUrl: String = "",
        ) : Event
        data class DubbingSelected(val index: Int, val currentPosMs: Long) : Event
        data class BalancerSelected(val index: Int, val currentPosMs: Long) : Event
        data class QualitySelected(val quality: String, val currentPosMs: Long) : Event
        data class SpeedSelected(val speed: Float) : Event
        data class ResizeModeSelected(val mode: PlayerResizeMode) : Event
        data class ZoomLevelSelected(val level: PlayerZoomLevel) : Event
        data class MobileVideoTransformChanged(
            val scale: Float,
            val offsetX: Float,
            val offsetY: Float,
        ) : Event

        data class PlaybackPositionChanged(
            val positionMs: Long,
            val durationMs: Long,
            val episodeUrl: String = "",
        ) : Event
        data class SaveProgress(val snapshot: PlayerProgressSnapshot) : Event
        data class SkipSegmentSelected(
            val type: PlayerSkipType,
            val fromMs: Long,
            val toMs: Long,
        ) : Event

        data class PlaybackError(
            val message: String,
            val errorCode: String? = null,
            val errorType: String? = null,
        ) : Event
        data object RetryStream : Event
        data object RateTitle : Event
    }

    sealed interface Effect : UiEffect
}

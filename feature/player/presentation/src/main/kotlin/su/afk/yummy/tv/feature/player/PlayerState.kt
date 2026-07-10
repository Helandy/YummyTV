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
        val autoPlayNextEpisode: Boolean = false,
        val mobileVideoScale: Float = 1f,
        val mobileVideoOffsetX: Float = 0f,
        val mobileVideoOffsetY: Float = 0f,
        val isOfflinePlayback: Boolean = false,
        val offlineCacheKey: String? = null,
    ) : UiState

    /** Пользовательские действия и события воспроизведения на экране плеера. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object Back : Event

        /** Пользователь открыл экран деталей текущего тайтла. */
        data object OpenDetails : Event

        /** Пользователь выбрал предыдущий эпизод. */
        data object PrevEpisode : Event

        /** Пользователь выбрал следующий эпизод из указанного источника. */
        data class NextEpisode(
            val source: PlayerNextEpisodeSource = PlayerNextEpisodeSource.Controls,
        ) : Event

        /** Воспроизведение эпизода дошло до завершения с текущей позицией и длительностью. */
        data class EpisodeCompleted(
            val positionMs: Long,
            val durationMs: Long,
            val episodeUrl: String = "",
        ) : Event

        /** Пользователь выбрал озвучку по индексу, сохранив текущую позицию. */
        data class DubbingSelected(val index: Int, val currentPosMs: Long) : Event

        /** Пользователь выбрал балансер по индексу, сохранив текущую позицию. */
        data class BalancerSelected(val index: Int, val currentPosMs: Long) : Event

        /** Пользователь выбрал качество потока, сохранив текущую позицию. */
        data class QualitySelected(val quality: String, val currentPosMs: Long) : Event

        /** Пользователь выбрал скорость воспроизведения. */
        data class SpeedSelected(val speed: Float) : Event

        /** Пользователь выбрал режим изменения размера видео. */
        data class ResizeModeSelected(val mode: PlayerResizeMode) : Event

        /** Пользователь выбрал уровень масштабирования видео. */
        data class ZoomLevelSelected(val level: PlayerZoomLevel) : Event

        /** Пользователь изменил масштаб и смещение видео на мобильном экране. */
        data class MobileVideoTransformChanged(
            val scale: Float,
            val offsetX: Float,
            val offsetY: Float,
        ) : Event

        /** Плеер сообщил текущую позицию, длительность и адрес эпизода. */
        data class PlaybackPositionChanged(
            val positionMs: Long,
            val durationMs: Long,
            val episodeUrl: String = "",
        ) : Event

        /** Плеер запросил сохранение снимка прогресса просмотра. */
        data class SaveProgress(val snapshot: PlayerProgressSnapshot) : Event

        /** Пользователь выбрал пропуск сегмента указанного типа и диапазона. */
        data class SkipSegmentSelected(
            val type: PlayerSkipType,
            val fromMs: Long,
            val toMs: Long,
        ) : Event

        /** Плеер сообщил об ошибке воспроизведения с техническими деталями. */
        data class PlaybackError(
            val message: String,
            val errorCode: String? = null,
            val errorType: String? = null,
        ) : Event

        /** Пользователь запросил повторное получение потока. */
        data object RetryStream : Event

        /** Мобильный плеер запросил фоновое обновление подписанного Alloha-потока. */
        /** Пользователь перешёл к оценке текущего тайтла. */
        data object RateTitle : Event

    }

    sealed interface Effect : UiEffect {
        data class ShowMessage(val message: String) : Effect
    }
}

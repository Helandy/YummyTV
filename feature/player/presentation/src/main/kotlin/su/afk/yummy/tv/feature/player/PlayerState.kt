package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.model.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.model.PlayerSkipType
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination

class PlayerState {
    data class State(
        val animeTitle: String = "",
        val animeId: Int = 0,
        val posterUrl: String = "",
        /** Обложка медиа-уведомления/медиа-сессии; считает VM через PlayerArtworkHandler. */
        val artworkUrl: String? = null,
        val sourceGraph: PlayerSourceGraph = PlayerSourceGraph(),
        val sourceSelection: PlayerSourceSelection = PlayerSourceSelection(),
        val dubbingResumeMs: Long = -1L,
        val retryKey: Int = 0,
        val streamUrl: String? = null,
        val streamHeaders: Map<String, String> = emptyMap(),
        val streamQualityMap: LinkedHashMap<String, String>? = null,
        val selectedQuality: String? = null,
        val allohaAudioTracks: List<AllohaAudioTrack> = emptyList(),
        val selectedAllohaAudioId: String? = null,
        val allohaSubtitles: List<AllohaSubtitleTrack> = emptyList(),
        /** Index into [allohaSubtitles]; null means subtitles are off. */
        val selectedAllohaSubtitleIndex: Int? = null,
        val selectedSpeed: Float = 1f,
        val resizeMode: PlayerResizeMode = PlayerResizeMode.FIT,
        val zoomLevel: PlayerZoomLevel = PlayerZoomLevel.PERCENT_10,
        val subtitleStyle: PlayerSubtitleStyleSettings = PlayerSubtitleStyleSettings(),
        val playerError: String? = null,
        val kodikBlockedError: String? = null,
        val resumeFromMs: Long = 0L,
        val playbackPositionMs: Long = 0L,
        val playbackDurationMs: Long = 0L,
        val autoSkipOpeningsEndings: Boolean = false,
        val autoSkipDelaySeconds: Int = 5,
        val showOpeningOnTimeline: Boolean = false,
        val autoPlayNextEpisode: Boolean = false,
        val nextEpisodeSwitchDelaySeconds: Int = 10,
        val pictureInPictureEnabled: Boolean = true,
        val playerOrientationMode: PlayerOrientationMode = PlayerOrientationMode.SYSTEM,
        val mobileGestureTutorialReady: Boolean = false,
        val showMobileGestureTutorial: Boolean = false,
        val tvControlsTutorialReady: Boolean = false,
        val showTvControlsTutorial: Boolean = false,
        val tvPlayerVolumeKeysEnabled: Boolean = false,
        val advancedPlayerVolumeEnabled: Boolean = false,
        val mobileVideoScale: Float = 1f,
        val mobileVideoOffsetX: Float = 0f,
        val mobileVideoOffsetY: Float = 0f,
        val isOfflinePlayback: Boolean = false,
        val isLocalFile: Boolean = false,
        val offlineCacheKey: String? = null,
        /** `VideoDownloadCacheKeyScheme.storageValue` скачанной серии; 0 — старая схема ключей. */
        val offlineCacheKeyScheme: Int = 0,
        val isPlaybackRecovering: Boolean = false,
        val showChangePlayerHint: Boolean = false,
        val finalEpisodeAction: PlayerFinalEpisodeAction = PlayerFinalEpisodeAction.Loading,
    ) : UiState

    /** Пользовательские действия и события воспроизведения на экране плеера. */
    sealed interface Event : UiEvent {
        /** Экран плеера переиспользуется для нового пункта назначения (навигация без пересоздания VM). */
        data class NavigateToDestination(val destination: PlayerDestination) : Event

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

        /**
         * Пользователь выбрал озвучку из собственного списка Alloha (шестерёнка её плеера) —
         * переключение потока внутри той же сессии, как смена качества.
         */
        data class AllohaAudioTrackSelected(val audioId: String, val currentPosMs: Long) : Event

        /** Пользователь выбрал субтитры Alloha (индекс в списке) или выключил их (null). */
        data class AllohaSubtitleSelected(val index: Int?) : Event

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
            val positionMs: Long = 0L,
        ) : Event

        /** Новый media item подготовлен после фонового восстановления воспроизведения. */
        data object PlaybackReady : Event

        /** Пользователь завершил одноразовое обучение жестам мобильного плеера. */
        data object MobileGestureTutorialDismissed : Event

        /** Пользователь завершил одноразовое обучение управлению ТВ-плеером. */
        data object TvControlsTutorialDismissed : Event

        /** Пользователь запросил повторное получение потока. */
        data object RetryStream : Event

        /** TV-приложение ушло в фон с открытым экраном плеера. */
        data object TvAppBackgrounded : Event

        /** Пользователь перешёл к оценке текущего тайтла. */
        data object RateTitle : Event

        /** Пользователь перешёл к управлению уведомлениями о новых сериях. */
        data object ManageSubscriptions : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowMessage(val message: String) : Effect
    }
}

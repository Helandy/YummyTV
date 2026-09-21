package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality

/** Поведение и настройки плеера: воспроизведение, жесты, громкость, размер кадра. */
interface PlayerSettingsStore {

    val preferredPlayer: Flow<PreferredPlayer>
    val preferredVideoQuality: Flow<PreferredVideoQuality>
    val autoSkipOpeningsEndings: Flow<Boolean>

    /** Задержка перед автопропуском опенинга/эндинга, сек (1..15). */
    val autoSkipDelaySeconds: Flow<Int>

    /** Показывать участок опенинга на полосе прогресса плеера. */
    val showOpeningOnTimeline: Flow<Boolean>
    val autoPlayNextEpisode: Flow<Boolean>

    /** Задержка перед авто-переключением на следующую серию, сек. 0 = мгновенно. */
    val nextEpisodeSwitchDelaySeconds: Flow<Int>

    /** Спрашивать озвучку при нажатии "Смотреть", вместо автовыбора самой популярной. */
    val askDubbingOnWatch: Flow<Boolean>
    val pictureInPictureEnabled: Flow<Boolean>

    /** Принудительная альбомная ориентация плеера, не зависящая от системной блокировки поворота. */
    val playerOrientationMode: Flow<PlayerOrientationMode>
    val suggestNextEpisodeOnWatched: Flow<Boolean>
    val refreshContinueWatchingProgressOnLaunch: Flow<Boolean>
    val mobilePlayerGestureTutorialDismissed: Flow<Boolean>
    val tvPlayerControlsTutorialDismissed: Flow<Boolean>
    val tvPlayerVolumeKeysEnabled: Flow<Boolean>
    val advancedPlayerVolumeEnabled: Flow<Boolean>
    val advancedPlayerVolumePercent: Flow<Int>

    /** Стабилизация громкости (сжатие динамического диапазона аудио). */
    val volumeStabilizationEnabled: Flow<Boolean>

    /** Размер оперативного буфера ExoPlayer: запас видео впереди позиции и лимит памяти под него. */
    val playerBufferProfile: Flow<PlayerBufferProfile>
    val playerResizeMode: Flow<PlayerResizeMode>
    val playerZoomLevel: Flow<PlayerZoomLevel>

    /** Оформление субтитров: размер, цвет, фон, смещение по вертикали. */
    val playerSubtitleStyle: Flow<PlayerSubtitleStyleSettings>

    fun playerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerResizeSettings>

    fun playerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerMobileVideoTransformSettings>

    suspend fun setPreferredPlayer(player: PreferredPlayer)
    suspend fun setPreferredVideoQuality(quality: PreferredVideoQuality)
    suspend fun setAutoSkipOpeningsEndings(enabled: Boolean)
    suspend fun setAutoSkipDelaySeconds(seconds: Int)
    suspend fun setShowOpeningOnTimeline(enabled: Boolean)
    suspend fun setAutoPlayNextEpisode(enabled: Boolean)
    suspend fun setNextEpisodeSwitchDelaySeconds(seconds: Int)
    suspend fun setAskDubbingOnWatch(enabled: Boolean)
    suspend fun setPictureInPictureEnabled(enabled: Boolean)
    suspend fun setPlayerOrientationMode(mode: PlayerOrientationMode)
    suspend fun setSuggestNextEpisodeOnWatched(enabled: Boolean)
    suspend fun setRefreshContinueWatchingProgressOnLaunch(enabled: Boolean)
    suspend fun dismissMobilePlayerGestureTutorial()
    suspend fun resetMobilePlayerGestureTutorial()
    suspend fun dismissTvPlayerControlsTutorial()
    suspend fun resetTvPlayerControlsTutorial()
    suspend fun setTvPlayerVolumeKeysEnabled(enabled: Boolean)
    suspend fun setAdvancedPlayerVolumeEnabled(enabled: Boolean)
    suspend fun setAdvancedPlayerVolumePercent(percent: Int)
    suspend fun setVolumeStabilizationEnabled(enabled: Boolean)
    suspend fun setPlayerBufferProfile(profile: PlayerBufferProfile)
    suspend fun setPlayerResizeMode(mode: PlayerResizeMode)
    suspend fun setPlayerZoomLevel(level: PlayerZoomLevel)
    suspend fun setPlayerSubtitleStyle(settings: PlayerSubtitleStyleSettings)

    suspend fun setPlayerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerResizeSettings,
    )

    suspend fun setPlayerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerMobileVideoTransformSettings,
    )
}

package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** Coordinates player-specific settings flows and persistence for the active anime/player scope. */
internal class PlayerSettingsHandler @Inject constructor(
    private val settingsStore: PlayerSettingsStore,
) {
    val autoSkipOpeningsEndings: Flow<Boolean> = settingsStore.autoSkipOpeningsEndings
    val autoSkipDelaySeconds: Flow<Int> = settingsStore.autoSkipDelaySeconds
    val showOpeningOnTimeline: Flow<Boolean> = settingsStore.showOpeningOnTimeline
    val autoPlayNextEpisode: Flow<Boolean> = settingsStore.autoPlayNextEpisode
    val nextEpisodeSwitchDelaySeconds: Flow<Int> = settingsStore.nextEpisodeSwitchDelaySeconds
    val pictureInPictureEnabled: Flow<Boolean> = settingsStore.pictureInPictureEnabled
    val playerOrientationMode: Flow<PlayerOrientationMode> = settingsStore.playerOrientationMode
    val mobilePlayerGestureTutorialDismissed: Flow<Boolean> =
        settingsStore.mobilePlayerGestureTutorialDismissed
    val tvPlayerControlsTutorialDismissed: Flow<Boolean> =
        settingsStore.tvPlayerControlsTutorialDismissed
    val tvPlayerVolumeKeysEnabled: Flow<Boolean> = settingsStore.tvPlayerVolumeKeysEnabled
    val advancedPlayerVolumeEnabled: Flow<Boolean> = settingsStore.advancedPlayerVolumeEnabled
    val playerSubtitleStyle: Flow<PlayerSubtitleStyleSettings> = settingsStore.playerSubtitleStyle

    suspend fun dismissMobilePlayerGestureTutorial() {
        settingsStore.dismissMobilePlayerGestureTutorial()
    }

    suspend fun dismissTvPlayerControlsTutorial() {
        settingsStore.dismissTvPlayerControlsTutorial()
    }

    fun observeResizeSettings(scope: PlayerResizeSettingsScope): Flow<PlayerResizeSettings> =
        settingsStore.playerResizeSettings(
            animeId = scope.animeId,
            animeTitle = scope.animeTitle,
            playerName = scope.playerName,
        )

    suspend fun saveResizeSettings(
        scope: PlayerResizeSettingsScope,
        settings: PlayerResizeSettings,
    ) {
        settingsStore.setPlayerResizeSettings(
            animeId = scope.animeId,
            animeTitle = scope.animeTitle,
            playerName = scope.playerName,
            settings = settings,
        )
    }

    fun observeMobileVideoTransformSettings(
        scope: PlayerResizeSettingsScope,
    ): Flow<PlayerMobileVideoTransformSettings> =
        settingsStore.playerMobileVideoTransformSettings(
            animeId = scope.animeId,
            animeTitle = scope.animeTitle,
            playerName = scope.playerName,
        )

    suspend fun saveMobileVideoTransformSettings(
        scope: PlayerResizeSettingsScope,
        settings: PlayerMobileVideoTransformSettings,
    ) {
        delay(MOBILE_VIDEO_TRANSFORM_SAVE_DEBOUNCE)
        settingsStore.setPlayerMobileVideoTransformSettings(
            animeId = scope.animeId,
            animeTitle = scope.animeTitle,
            playerName = scope.playerName,
            settings = settings,
        )
    }

    private companion object {
        val MOBILE_VIDEO_TRANSFORM_SAVE_DEBOUNCE = 250.milliseconds
    }
}

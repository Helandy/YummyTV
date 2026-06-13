package su.afk.yummy.tv.feature.player

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.preferences.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import javax.inject.Inject

/** Coordinates player-specific settings flows and persistence for the active anime/player scope. */
internal class PlayerSettingsHandler @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    val autoSkipOpeningsEndings: Flow<Boolean> = settingsStore.autoSkipOpeningsEndings

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
        delay(MOBILE_VIDEO_TRANSFORM_SAVE_DEBOUNCE_MS)
        settingsStore.setPlayerMobileVideoTransformSettings(
            animeId = scope.animeId,
            animeTitle = scope.animeTitle,
            playerName = scope.playerName,
            settings = settings,
        )
    }

    private companion object {
        const val MOBILE_VIDEO_TRANSFORM_SAVE_DEBOUNCE_MS = 250L
    }
}

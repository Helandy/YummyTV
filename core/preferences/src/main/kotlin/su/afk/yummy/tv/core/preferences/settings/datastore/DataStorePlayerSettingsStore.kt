package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.advancedPlayerVolumeEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.advancedPlayerVolumePercentKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.askDubbingOnWatchKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoPlayNextEpisodeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoSkipDelaySecondsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoSkipOpeningsEndingsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.mobilePlayerGestureTutorialDismissedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.nextEpisodeSwitchDelaySecondsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.pictureInPictureEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerBufferProfileKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerOrientationModeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerResizeModeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerZoomLevelKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.preferredPlayerKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.preferredVideoQualityKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.refreshContinueWatchingProgressOnLaunchKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showOpeningOnTimelineKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.subtitleBackgroundKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.subtitleOffsetKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.subtitleTextColorKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.subtitleTextSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.suggestNextEpisodeOnWatchedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.tvPlayerControlsTutorialDismissedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.tvPlayerVolumeKeysEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.volumeStabilizationEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchedLongRemainingMinutesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchedMediumRemainingMinutesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchedShortRemainingMinutesKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStorePlayerSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : PlayerSettingsStore {

    override val preferredPlayer: Flow<PreferredPlayer> =
        store.enumFlow(preferredPlayerKey, PreferredPlayer.NONE)

    override val preferredVideoQuality: Flow<PreferredVideoQuality> =
        store.enumFlow(preferredVideoQualityKey, PreferredVideoQuality.BEST)

    override val autoSkipOpeningsEndings: Flow<Boolean> =
        store.boolean(autoSkipOpeningsEndingsKey, false)

    override val autoSkipDelaySeconds: Flow<Int> = store.data.map { prefs ->
        (prefs[autoSkipDelaySecondsKey] ?: DEFAULT_AUTO_SKIP_DELAY_SECONDS)
            .coerceIn(MIN_AUTO_SKIP_DELAY_SECONDS, MAX_AUTO_SKIP_DELAY_SECONDS)
    }

    override val showOpeningOnTimeline: Flow<Boolean> =
        store.boolean(showOpeningOnTimelineKey, false)

    override val autoPlayNextEpisode: Flow<Boolean> =
        store.boolean(autoPlayNextEpisodeKey, false)

    // coerceIn и на чтении тоже: значение могло быть записано более старой версией без клампа.
    override val nextEpisodeSwitchDelaySeconds: Flow<Int> = store.data.map { prefs ->
        (prefs[nextEpisodeSwitchDelaySecondsKey] ?: DEFAULT_NEXT_EPISODE_SWITCH_DELAY_SECONDS)
            .coerceIn(0, MAX_NEXT_EPISODE_SWITCH_DELAY_SECONDS)
    }

    override val askDubbingOnWatch: Flow<Boolean> = store.boolean(askDubbingOnWatchKey, true)

    override val pictureInPictureEnabled: Flow<Boolean> =
        store.boolean(pictureInPictureEnabledKey, true)

    override val playerOrientationMode: Flow<PlayerOrientationMode> =
        store.enumFlow(playerOrientationModeKey, PlayerOrientationMode.SYSTEM)

    override val suggestNextEpisodeOnWatched: Flow<Boolean> =
        store.boolean(suggestNextEpisodeOnWatchedKey, true)

    override val watchedThresholds: Flow<WatchedThresholds> =
        store.data.map { prefs -> prefs.watchedThresholds() }.distinctUntilChanged()

    override val refreshContinueWatchingProgressOnLaunch: Flow<Boolean> =
        store.boolean(refreshContinueWatchingProgressOnLaunchKey, false)

    override val mobilePlayerGestureTutorialDismissed: Flow<Boolean> =
        store.boolean(mobilePlayerGestureTutorialDismissedKey, false)

    override val tvPlayerControlsTutorialDismissed: Flow<Boolean> =
        store.boolean(tvPlayerControlsTutorialDismissedKey, false)

    override val tvPlayerVolumeKeysEnabled: Flow<Boolean> =
        store.boolean(tvPlayerVolumeKeysEnabledKey, false)

    override val advancedPlayerVolumeEnabled: Flow<Boolean> =
        store.boolean(advancedPlayerVolumeEnabledKey, false)

    // coerceIn и на чтении тоже: значение могло быть записано более старой версией без клампа.
    override val advancedPlayerVolumePercent: Flow<Int> = store.data.map { prefs ->
        (prefs[advancedPlayerVolumePercentKey] ?: DEFAULT_VOLUME_PERCENT)
            .coerceIn(0, MAX_VOLUME_PERCENT)
    }

    override val volumeStabilizationEnabled: Flow<Boolean> =
        store.boolean(volumeStabilizationEnabledKey, false)

    override val playerBufferProfile: Flow<PlayerBufferProfile> =
        store.enumFlow(playerBufferProfileKey, PlayerBufferProfile.SMALL)

    override val playerResizeMode: Flow<PlayerResizeMode> =
        store.enumFlow(playerResizeModeKey, PlayerResizeMode.FIT)

    override val playerZoomLevel: Flow<PlayerZoomLevel> =
        store.enumFlow(playerZoomLevelKey, PlayerZoomLevel.PERCENT_10)

    // Четыре ключа читаются одним map'ом, а не combine'ом четырёх Flow: источник один и тот же
    // (store.data), так что лишние комбинаторы только добавили бы промежуточных эмитов.
    override val playerSubtitleStyle: Flow<PlayerSubtitleStyleSettings> = store.data.map { prefs ->
        PlayerSubtitleStyleSettings(
            textSize = (prefs[subtitleTextSizeKey] ?: DEFAULT_SUBTITLE_TEXT_SIZE_PERCENT)
                .coerceIn(MIN_SUBTITLE_TEXT_SIZE_PERCENT, MAX_SUBTITLE_TEXT_SIZE_PERCENT),
            textColor = prefs.enum(subtitleTextColorKey, PlayerSubtitleTextColor.WHITE),
            background = prefs.enum(subtitleBackgroundKey, PlayerSubtitleBackground.TRANSLUCENT),
            offset = (prefs[subtitleOffsetKey] ?: DEFAULT_SUBTITLE_OFFSET_PERCENT)
                .coerceIn(MIN_SUBTITLE_OFFSET_PERCENT, MAX_SUBTITLE_OFFSET_PERCENT),
        )
    }

    override fun playerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerResizeSettings> {
        val key = playerScopedResizeSettingsKey(animeId, animeTitle, playerName)
        return store.data.map { prefs ->
            prefs[key]?.toPlayerResizeSettings() ?: PlayerResizeSettings()
        }
    }

    override fun playerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerMobileVideoTransformSettings> {
        val key = playerScopedMobileVideoTransformSettingsKey(animeId, animeTitle, playerName)
        return store.data.map { prefs ->
            prefs[key]?.toPlayerMobileVideoTransformSettings()
                ?: PlayerMobileVideoTransformSettings()
        }
    }

    override suspend fun setPreferredPlayer(player: PreferredPlayer) =
        store.setEnum(preferredPlayerKey, player)

    override suspend fun setPreferredVideoQuality(quality: PreferredVideoQuality) =
        store.setEnum(preferredVideoQualityKey, quality)

    override suspend fun setAutoSkipOpeningsEndings(enabled: Boolean) =
        store.setBoolean(autoSkipOpeningsEndingsKey, enabled)

    override suspend fun setAutoSkipDelaySeconds(seconds: Int) {
        store.edit { prefs ->
            prefs[autoSkipDelaySecondsKey] =
                seconds.coerceIn(MIN_AUTO_SKIP_DELAY_SECONDS, MAX_AUTO_SKIP_DELAY_SECONDS)
        }
    }

    override suspend fun setShowOpeningOnTimeline(enabled: Boolean) =
        store.setBoolean(showOpeningOnTimelineKey, enabled)

    override suspend fun setAutoPlayNextEpisode(enabled: Boolean) =
        store.setBoolean(autoPlayNextEpisodeKey, enabled)

    override suspend fun setNextEpisodeSwitchDelaySeconds(seconds: Int) {
        store.edit { prefs ->
            prefs[nextEpisodeSwitchDelaySecondsKey] =
                seconds.coerceIn(0, MAX_NEXT_EPISODE_SWITCH_DELAY_SECONDS)
        }
    }

    override suspend fun setAskDubbingOnWatch(enabled: Boolean) =
        store.setBoolean(askDubbingOnWatchKey, enabled)

    override suspend fun setPictureInPictureEnabled(enabled: Boolean) =
        store.setBoolean(pictureInPictureEnabledKey, enabled)

    override suspend fun setPlayerOrientationMode(mode: PlayerOrientationMode) =
        store.setEnum(playerOrientationModeKey, mode)

    override suspend fun setSuggestNextEpisodeOnWatched(enabled: Boolean) =
        store.setBoolean(suggestNextEpisodeOnWatchedKey, enabled)

    override suspend fun setWatchedThresholds(thresholds: WatchedThresholds) {
        val coerced = thresholds.coerced()
        store.edit { prefs ->
            prefs[watchedShortRemainingMinutesKey] = coerced.shortMinutes
            prefs[watchedMediumRemainingMinutesKey] = coerced.mediumMinutes
            prefs[watchedLongRemainingMinutesKey] = coerced.longMinutes
        }
    }

    override suspend fun setRefreshContinueWatchingProgressOnLaunch(enabled: Boolean) =
        store.setBoolean(refreshContinueWatchingProgressOnLaunchKey, enabled)

    override suspend fun dismissMobilePlayerGestureTutorial() =
        store.setBoolean(mobilePlayerGestureTutorialDismissedKey, true)

    override suspend fun resetMobilePlayerGestureTutorial() =
        store.setBoolean(mobilePlayerGestureTutorialDismissedKey, false)

    override suspend fun dismissTvPlayerControlsTutorial() =
        store.setBoolean(tvPlayerControlsTutorialDismissedKey, true)

    override suspend fun resetTvPlayerControlsTutorial() =
        store.setBoolean(tvPlayerControlsTutorialDismissedKey, false)

    override suspend fun setTvPlayerVolumeKeysEnabled(enabled: Boolean) =
        store.setBoolean(tvPlayerVolumeKeysEnabledKey, enabled)

    override suspend fun setAdvancedPlayerVolumeEnabled(enabled: Boolean) =
        store.setBoolean(advancedPlayerVolumeEnabledKey, enabled)

    override suspend fun setAdvancedPlayerVolumePercent(percent: Int) {
        store.edit { prefs ->
            prefs[advancedPlayerVolumePercentKey] = percent.coerceIn(0, MAX_VOLUME_PERCENT)
        }
    }

    override suspend fun setVolumeStabilizationEnabled(enabled: Boolean) =
        store.setBoolean(volumeStabilizationEnabledKey, enabled)

    override suspend fun setPlayerBufferProfile(profile: PlayerBufferProfile) =
        store.setEnum(playerBufferProfileKey, profile)

    override suspend fun setPlayerResizeMode(mode: PlayerResizeMode) =
        store.setEnum(playerResizeModeKey, mode)

    override suspend fun setPlayerZoomLevel(level: PlayerZoomLevel) =
        store.setEnum(playerZoomLevelKey, level)

    override suspend fun setPlayerSubtitleStyle(settings: PlayerSubtitleStyleSettings) {
        store.edit { prefs ->
            prefs[subtitleTextSizeKey] = settings.textSize
                .coerceIn(MIN_SUBTITLE_TEXT_SIZE_PERCENT, MAX_SUBTITLE_TEXT_SIZE_PERCENT)
            prefs[subtitleTextColorKey] = settings.textColor.name
            prefs[subtitleBackgroundKey] = settings.background.name
            prefs[subtitleOffsetKey] = settings.offset
                .coerceIn(MIN_SUBTITLE_OFFSET_PERCENT, MAX_SUBTITLE_OFFSET_PERCENT)
        }
    }

    override suspend fun setPlayerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerResizeSettings,
    ) {
        val key = playerScopedResizeSettingsKey(animeId, animeTitle, playerName)
        store.edit { prefs -> prefs[key] = settings.toPreferenceValue() }
    }

    override suspend fun setPlayerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerMobileVideoTransformSettings,
    ) {
        val key = playerScopedMobileVideoTransformSettingsKey(animeId, animeTitle, playerName)
        store.edit { prefs -> prefs[key] = settings.toPreferenceValue() }
    }

    private companion object {
        const val DEFAULT_VOLUME_PERCENT = 100
        const val MAX_VOLUME_PERCENT = 100
        const val DEFAULT_AUTO_SKIP_DELAY_SECONDS = 5
        const val MIN_AUTO_SKIP_DELAY_SECONDS = 1
        const val MAX_AUTO_SKIP_DELAY_SECONDS = 15
        const val DEFAULT_NEXT_EPISODE_SWITCH_DELAY_SECONDS = 10
        const val MAX_NEXT_EPISODE_SWITCH_DELAY_SECONDS = 30
        const val DEFAULT_SUBTITLE_TEXT_SIZE_PERCENT = 100
        const val MIN_SUBTITLE_TEXT_SIZE_PERCENT = 50
        const val MAX_SUBTITLE_TEXT_SIZE_PERCENT = 200
        const val DEFAULT_SUBTITLE_OFFSET_PERCENT = 6
        const val MIN_SUBTITLE_OFFSET_PERCENT = 0
        const val MAX_SUBTITLE_OFFSET_PERCENT = 20

        /** Настройки кадра привязаны к паре «тайтл + плеер», отсюда составной ключ. */
        fun playerScopedResizeSettingsKey(
            animeId: Int,
            animeTitle: String,
            playerName: String,
        ): Preferences.Key<String> = stringPreferencesKey(
            "player_resize_settings|${titleKeyPart(animeId, animeTitle)}" +
                    "|player:${playerName.normalizedPlayerResizeKeyPart()}"
        )

        fun playerScopedMobileVideoTransformSettingsKey(
            animeId: Int,
            animeTitle: String,
            playerName: String,
        ): Preferences.Key<String> = stringPreferencesKey(
            "player_mobile_video_transform|${titleKeyPart(animeId, animeTitle)}" +
                    "|player:${playerName.normalizedPlayerResizeKeyPart()}"
        )

        fun titleKeyPart(animeId: Int, animeTitle: String): String =
            if (animeId > 0) {
                "anime_id:$animeId"
            } else {
                "title:${animeTitle.normalizedPlayerResizeKeyPart()}"
            }

        fun String.normalizedPlayerResizeKeyPart(): String =
            trim()
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .ifBlank { "unknown" }

        fun PlayerResizeSettings.toPreferenceValue(): String =
            "${resizeMode.name}|${zoomLevel.name}"

        fun String.toPlayerResizeSettings(): PlayerResizeSettings? {
            val parts = split('|')
            val mode = parts.getOrNull(0)?.let { name ->
                runCatching { PlayerResizeMode.valueOf(name) }.getOrNull()
            } ?: return null
            val level = parts.getOrNull(1)?.let { name ->
                runCatching { PlayerZoomLevel.valueOf(name) }.getOrNull()
            } ?: PlayerZoomLevel.PERCENT_10
            return PlayerResizeSettings(resizeMode = mode, zoomLevel = level)
        }

        fun PlayerMobileVideoTransformSettings.toPreferenceValue(): String =
            "$scale|$offsetX|$offsetY"

        fun String.toPlayerMobileVideoTransformSettings(): PlayerMobileVideoTransformSettings? {
            val parts = split('|')
            val scale = parts.getOrNull(0)?.toFloatOrNull() ?: return null
            return PlayerMobileVideoTransformSettings(
                scale = scale,
                offsetX = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
                offsetY = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
            )
        }
    }
}

package su.afk.yummy.tv.feature.playersetup.handler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
import javax.inject.Inject

/** Читает и записывает настройки плеера, которые предлагаются на первичной настройке. */
internal class PlayerSetupSettingsHandler @Inject constructor(
    private val store: PlayerSettingsStore,
    private val accountStore: YaniAccountSettingsStore,
) {

    fun observe(): Flow<PlayerSetupState.State> = combine(
        combine(
            store.showOpeningOnTimeline,
            store.autoSkipOpeningsEndings,
            store.autoPlayNextEpisode,
            store.suggestNextEpisodeOnWatched,
            store.askDubbingOnWatch,
        ) { showOpening, autoSkip, autoPlay, suggestNext, askDubbing ->
            PlayerSetupState.State(
                showOpeningOnTimeline = showOpening,
                autoSkipOpeningsEndings = autoSkip,
                autoPlayNextEpisode = autoPlay,
                suggestNextEpisodeOnWatched = suggestNext,
                askDubbingOnWatch = askDubbing,
            )
        },
        combine(
            store.preferredVideoQuality,
            store.playerOrientationMode,
            store.pictureInPictureEnabled,
            store.refreshContinueWatchingProgressOnLaunch,
            accountStore.yaniContentLanguage,
        ) { quality, orientation, pip, refreshProgress, language ->
            PlayerSetupState.State(
                preferredVideoQuality = quality,
                playerOrientationMode = orientation,
                pictureInPictureEnabled = pip,
                refreshContinueWatchingProgressOnLaunch = refreshProgress,
                contentLanguage = language,
            )
        },
    ) { toggles, playback ->
        toggles.copy(
            preferredVideoQuality = playback.preferredVideoQuality,
            playerOrientationMode = playback.playerOrientationMode,
            pictureInPictureEnabled = playback.pictureInPictureEnabled,
            refreshContinueWatchingProgressOnLaunch = playback.refreshContinueWatchingProgressOnLaunch,
            contentLanguage = playback.contentLanguage,
        )
    }

    suspend fun setShowOpeningOnTimeline(enabled: Boolean) = store.setShowOpeningOnTimeline(enabled)

    suspend fun setAutoSkipOpeningsEndings(enabled: Boolean) = store.setAutoSkipOpeningsEndings(enabled)

    suspend fun setAutoPlayNextEpisode(enabled: Boolean) = store.setAutoPlayNextEpisode(enabled)

    suspend fun setSuggestNextEpisodeOnWatched(enabled: Boolean) =
        store.setSuggestNextEpisodeOnWatched(enabled)

    suspend fun setAskDubbingOnWatch(enabled: Boolean) = store.setAskDubbingOnWatch(enabled)

    suspend fun setPictureInPictureEnabled(enabled: Boolean) = store.setPictureInPictureEnabled(enabled)

    suspend fun setRefreshContinueWatchingProgressOnLaunch(enabled: Boolean) =
        store.setRefreshContinueWatchingProgressOnLaunch(enabled)

    suspend fun setContentLanguage(language: YaniContentLanguage) =
        accountStore.setYaniContentLanguage(language)

    suspend fun setPreferredVideoQuality(quality: PreferredVideoQuality) =
        store.setPreferredVideoQuality(quality)

    suspend fun setPlayerOrientationMode(mode: PlayerOrientationMode) =
        store.setPlayerOrientationMode(mode)
}

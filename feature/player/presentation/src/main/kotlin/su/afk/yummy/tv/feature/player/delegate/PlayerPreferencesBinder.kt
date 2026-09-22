package su.afk.yummy.tv.feature.player.delegate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import javax.inject.Inject

/** Держит глобальные настройки плеера и одноразовые обучения в состоянии экрана. */
internal class PlayerPreferencesBinder @Inject constructor(
    private val settings: PlayerSettingsHandler,
) {
    fun bind(host: PlayerStateHost) {
        with(settings) {
            host.bind(autoSkipOpeningsEndings) { copy(autoSkipOpeningsEndings = it) }
            host.bind(autoSkipDelaySeconds) { copy(autoSkipDelaySeconds = it) }
            host.bind(showOpeningOnTimeline) { copy(showOpeningOnTimeline = it) }
            host.bind(autoPlayNextEpisode) { copy(autoPlayNextEpisode = it) }
            host.bind(nextEpisodeSwitchDelaySeconds) { copy(nextEpisodeSwitchDelaySeconds = it) }
            host.bind(pictureInPictureEnabled) { copy(pictureInPictureEnabled = it) }
            host.bind(playerOrientationMode) { copy(playerOrientationMode = it) }
            host.bind(mobilePlayerGestureTutorialDismissed) { dismissed ->
                copy(mobileGestureTutorialReady = true, showMobileGestureTutorial = !dismissed)
            }
            host.bind(tvPlayerControlsTutorialDismissed) { dismissed ->
                copy(tvControlsTutorialReady = true, showTvControlsTutorial = !dismissed)
            }
            host.bind(tvPlayerVolumeKeysEnabled) { copy(tvPlayerVolumeKeysEnabled = it) }
            host.bind(advancedPlayerVolumeEnabled) { copy(advancedPlayerVolumeEnabled = it) }
            host.bind(playerSubtitleStyle) { copy(subtitleStyle = it) }
        }
    }

    fun dismissMobileGestureTutorial(host: PlayerStateHost) {
        host.update { copy(showMobileGestureTutorial = false) }
        host.scope.launch { settings.dismissMobilePlayerGestureTutorial() }
    }

    fun dismissTvControlsTutorial(host: PlayerStateHost) {
        host.update { copy(showTvControlsTutorial = false) }
        host.scope.launch { settings.dismissTvPlayerControlsTutorial() }
    }

    private fun <T> PlayerStateHost.bind(
        flow: Flow<T>,
        reducer: PlayerState.State.(T) -> PlayerState.State,
    ) {
        flow.onEach { value -> update { reducer(value) } }.launchIn(scope)
    }
}

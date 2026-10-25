package su.afk.yummy.tv.feature.player.delegate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import su.afk.yummy.tv.feature.player.utils.QualityAdaptationPolicy
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
            host.bind(playerControlsAutoHideSeconds) { copy(controlsAutoHideSeconds = it) }
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
            host.bind(volumeStabilizationEnabled) { copy(volumeStabilizationEnabled = it) }
            host.bind(adaptiveVideoQualityEnabled) { copy(adaptiveQualityEnabled = it) }
            host.bind(preferredVideoQuality) {
                copy(adaptiveCeilingHeight = it.height ?: QualityAdaptationPolicy.DEFAULT_CEILING_HEIGHT)
            }
            host.bind(playerSubtitleStyle) { copy(subtitleStyle = it) }
            host.bind(playerPlaybackSpeed) { copy(selectedSpeed = it) }
        }
    }

    fun selectOrientationMode(host: PlayerStateHost, mode: PlayerOrientationMode) {
        host.scope.launch { settings.saveOrientationMode(mode) }
    }

    /** Скорость запоминается глобально, чтобы следующая серия и тайтл начинались с неё же. */
    fun savePlaybackSpeed(host: PlayerStateHost, speed: Float) {
        host.scope.launch { settings.savePlaybackSpeed(speed) }
    }

    /** Стиль субтитров общий с экраном настроек; оверлей подхватит его из DataStore-флоу. */
    fun saveSubtitleStyle(host: PlayerStateHost, style: PlayerSubtitleStyleSettings) {
        host.update { copy(subtitleStyle = style) }
        host.scope.launch { settings.saveSubtitleStyle(style) }
    }

    fun setVolumeStabilization(host: PlayerStateHost, enabled: Boolean) {
        host.update { copy(volumeStabilizationEnabled = enabled) }
        host.scope.launch { settings.saveVolumeStabilizationEnabled(enabled) }
    }

    /** Включение снимает ручную блокировку серии: адаптация снова управляет качеством. */
    fun setAdaptiveQuality(host: PlayerStateHost, enabled: Boolean) {
        host.update {
            copy(
                adaptiveQualityEnabled = enabled,
                manualQualityEpisode = if (enabled) null else manualQualityEpisode,
            )
        }
        host.scope.launch { settings.saveAdaptiveVideoQualityEnabled(enabled) }
    }

    /** Лимит адаптации из плеера общий с настройками: «Предпочитаемое качество» — он же потолок. */
    fun setAdaptiveLimit(host: PlayerStateHost, quality: PreferredVideoQuality) {
        host.update {
            copy(adaptiveCeilingHeight = quality.height ?: QualityAdaptationPolicy.DEFAULT_CEILING_HEIGHT)
        }
        host.scope.launch { settings.savePreferredVideoQuality(quality) }
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

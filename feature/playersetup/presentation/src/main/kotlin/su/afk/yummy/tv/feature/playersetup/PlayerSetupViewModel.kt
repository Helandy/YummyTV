package su.afk.yummy.tv.feature.playersetup

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.playersetup.handler.PlayerSetupSettingsHandler
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject

@HiltViewModel
class PlayerSetupViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val settingsHandler: PlayerSetupSettingsHandler,
    private val settingsNavigator: ISettingsNavigator,
) : BaseViewModel<PlayerSetupState.State, PlayerSetupState.Event, PlayerSetupState.Effect>() {

    override fun createInitialState() = PlayerSetupState.State()

    init {
        settingsHandler.observe()
            .onEach { settings -> setState { settings } }
            .launchIn(viewModelScope)
        launchWrite { settingsHandler.detectPosterQualityIfNeeded() }
    }

    override fun onEvent(event: PlayerSetupState.Event) {
        when (event) {
            PlayerSetupState.Event.ShowOpeningOnTimelineToggled -> launchWrite {
                settingsHandler.setShowOpeningOnTimeline(!currentState.showOpeningOnTimeline)
            }

            PlayerSetupState.Event.AutoSkipOpeningsEndingsToggled -> launchWrite {
                settingsHandler.setAutoSkipOpeningsEndings(!currentState.autoSkipOpeningsEndings)
            }

            PlayerSetupState.Event.AutoPlayNextEpisodeToggled -> launchWrite {
                settingsHandler.setAutoPlayNextEpisode(!currentState.autoPlayNextEpisode)
            }

            PlayerSetupState.Event.SuggestNextEpisodeOnWatchedToggled -> launchWrite {
                settingsHandler.setSuggestNextEpisodeOnWatched(!currentState.suggestNextEpisodeOnWatched)
            }

            PlayerSetupState.Event.PictureInPictureToggled -> launchWrite {
                settingsHandler.setPictureInPictureEnabled(!currentState.pictureInPictureEnabled)
            }

            PlayerSetupState.Event.AdaptiveVideoQualityToggled -> launchWrite {
                settingsHandler.setAdaptiveVideoQualityEnabled(!currentState.adaptiveVideoQualityEnabled)
            }

            is PlayerSetupState.Event.ContentLanguageSelected -> launchWrite {
                settingsHandler.setContentLanguage(event.language)
            }

            is PlayerSetupState.Event.PreferredVideoQualitySelected -> launchWrite {
                settingsHandler.setPreferredVideoQuality(event.quality)
            }

            is PlayerSetupState.Event.PlayerOrientationModeSelected -> launchWrite {
                settingsHandler.setPlayerOrientationMode(event.mode)
            }

            PlayerSetupState.Event.DoneSelected -> nav.back()
            PlayerSetupState.Event.MoreSettingsSelected -> {
                nav.back()
                nav.navigate(settingsNavigator.getSettingsDest())
            }
        }
    }

    private fun launchWrite(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

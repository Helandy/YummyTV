package su.afk.yummy.tv.feature.watchlater

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.watchlater.usecase.ObserveWatchLaterUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.RemoveWatchLaterEpisodeUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class WatchLaterViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val observeWatchLater: ObserveWatchLaterUseCase,
    private val removeWatchLaterEpisode: RemoveWatchLaterEpisodeUseCase,
    private val detailsNavigator: IDetailsNavigator,
) : BaseViewModel<WatchLaterState.State, WatchLaterState.Event, WatchLaterState.Effect>() {

    init {
        observeWatchLater()
            .onEach { items -> setState { copy(items = items.toImmutableList()) } }
            .launchIn(viewModelScope)
    }

    override fun createInitialState() = WatchLaterState.State()

    override fun onEvent(event: WatchLaterState.Event) {
        when (event) {
            WatchLaterState.Event.BackSelected -> nav.back()

            // Экран серий сам запустит воспроизведение или покажет выбор озвучки —
            // см. consumePendingEpisode в EpisodesViewModel.
            is WatchLaterState.Event.ItemSelected -> nav.navigate(
                detailsNavigator.getEpisodesDest(
                    animeId = event.animeId,
                    pendingEpisode = event.episode,
                )
            )

            is WatchLaterState.Event.RemoveSelected -> viewModelScope.launch {
                removeWatchLaterEpisode(event.animeId, event.episode)
            }
        }
    }
}

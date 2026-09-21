package su.afk.yummy.tv.feature.details.viewingorder

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator

@HiltViewModel(assistedFactory = ViewingOrderViewModel.Factory::class)
class ViewingOrderViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<ViewingOrderState.State, ViewingOrderState.Event, ViewingOrderState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): ViewingOrderViewModel
    }

    override fun createInitialState() = ViewingOrderState.State(currentAnimeId = animeId)

    init {
        analytics.eventViewingOrderScreenOpened(animeId)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: ViewingOrderState.Event) {
        when (event) {
            ViewingOrderState.Event.BackSelected -> nav.back()
            is ViewingOrderState.Event.AnimeSelected -> {
                analytics.eventViewingOrderAnimeSelected(animeId, event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            ViewingOrderState.Event.RetrySelected -> viewModelScope.launch { load() }
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeDetails(animeId) }.fold(
            onSuccess = { details ->
                setState { copy(isLoading = false, items = details.viewingOrder.toImmutableList()) }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = e.userMessage()) } },
        )
    }
}

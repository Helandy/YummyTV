package su.afk.yummy.tv.feature.details.viewingorder

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator

@HiltViewModel(assistedFactory = ViewingOrderViewModel.Factory::class)
class ViewingOrderViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<ViewingOrderState.State, ViewingOrderState.Event, ViewingOrderState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): ViewingOrderViewModel
    }

    override fun createInitialState() = ViewingOrderState.State(currentAnimeId = animeId)

    init {
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: ViewingOrderState.Event) {
        when (event) {
            ViewingOrderState.Event.BackSelected -> nav.back()
            is ViewingOrderState.Event.AnimeSelected -> {
                analytics.eventViewingOrderAnimeSelected(animeId, event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeDetails(animeId) }.fold(
            onSuccess = { details -> setState { copy(isLoading = false, items = details.viewingOrder) } },
            onFailure = { e -> setState { copy(isLoading = false, error = e.message) } },
        )
    }
}

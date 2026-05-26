package su.afk.yummy.tv.feature.details

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
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTrailersUseCase

@HiltViewModel(assistedFactory = TrailersViewModel.Factory::class)
class TrailersViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeTrailers: GetAnimeTrailersUseCase,
) : BaseViewModelNew<TrailersState.State, TrailersState.Event, TrailersState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): TrailersViewModel
    }

    override fun createInitialState() = TrailersState.State()

    init {
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: TrailersState.Event) {
        when (event) {
            TrailersState.Event.BackSelected -> nav.back()
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true) }
        runCatching { getAnimeTrailers(animeId) }.fold(
            onSuccess = { trailers -> setState { copy(isLoading = false, trailers = trailers) } },
            onFailure = { setState { copy(isLoading = false) } },
        )
    }
}

package su.afk.yummy.tv.feature.details.similar

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
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeRecommendationsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.SimilarUiState

@HiltViewModel(assistedFactory = SimilarViewModel.Factory::class)
class SimilarViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeRecommendations: GetAnimeRecommendationsUseCase,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<SimilarState.State, SimilarState.Event, SimilarState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): SimilarViewModel
    }

    override fun createInitialState() = SimilarState.State()

    init {
        analytics.eventSimilarScreenOpened(animeId)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: SimilarState.Event) {
        when (event) {
            SimilarState.Event.BackSelected -> nav.back()
            is SimilarState.Event.AnimeSelected -> {
                analytics.eventSimilarAnimeSelected(animeId, event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
            is SimilarState.Event.SourceSelected -> selectSource(event.fromAi)
            SimilarState.Event.SourceToggled -> selectSource(!currentState.fromAi)
            SimilarState.Event.RetrySelected -> viewModelScope.launch { load() }
        }
    }

    private fun selectSource(fromAi: Boolean) {
        if (currentState.fromAi == fromAi) return
        analytics.eventSimilarSourceSelected(animeId, fromAi)
        setState { copy(fromAi = fromAi) }
        viewModelScope.launch { load(fromAi) }
    }

    private suspend fun load(fromAi: Boolean = currentState.fromAi) {
        setState { copy(similarState = SimilarUiState.Loading) }
        runCatching { getAnimeRecommendations(animeId, fromAi) }.fold(
            onSuccess = { items ->
                setState {
                    if (this.fromAi == fromAi) {
                        val nextState = if (items.isEmpty()) {
                            SimilarUiState.Empty
                        } else {
                            SimilarUiState.Content(items)
                        }
                        copy(similarState = nextState)
                    } else {
                        this
                    }
                }
            },
            onFailure = {
                setState {
                    if (this.fromAi == fromAi) {
                        copy(similarState = SimilarUiState.Error(it.message))
                    } else {
                        this
                    }
                }
            },
        )
    }

}

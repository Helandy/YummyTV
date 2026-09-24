package su.afk.yummy.tv.feature.details.full

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationKind
import su.afk.yummy.tv.feature.details.presentation.R

@HiltViewModel(assistedFactory = FullDetailsViewModel.Factory::class)
class FullDetailsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val stringProvider: StringProvider,
    private val analytics: DetailsAnalytics,
    private val detailsNavigator: IDetailsNavigator,
) : BaseViewModel<FullDetailsState.State, FullDetailsState.Event, FullDetailsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): FullDetailsViewModel
    }

    override fun createInitialState() = FullDetailsState.State()

    init {
        analytics.eventFullScreenOpened(animeId)
        load()
    }

    override fun onEvent(event: FullDetailsState.Event) {
        when (event) {
            FullDetailsState.Event.BackSelected -> nav.back()
            FullDetailsState.Event.RetrySelected -> {
                analytics.eventFullRetry(animeId)
                load()
            }

            is FullDetailsState.Event.GenreSelected -> nav.navigate(
                detailsNavigator.getRelationDest(DetailsRelationKind.GENRE, event.id)
            )

            is FullDetailsState.Event.StudioSelected -> nav.navigate(
                detailsNavigator.getRelationDest(
                    kind = DetailsRelationKind.STUDIO,
                    id = event.id,
                    url = event.url,
                )
            )

            is FullDetailsState.Event.DirectorSelected -> nav.navigate(
                detailsNavigator.getRelationDest(DetailsRelationKind.DIRECTOR, event.id)
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runSuspendCatching { getAnimeDetails(animeId) }.fold(
                onSuccess = { details -> setState { copy(isLoading = false, details = details) } },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.userMessage(stringProvider.get(R.string.details_load_error)),
                        )
                    }
                },
            )
        }
    }
}

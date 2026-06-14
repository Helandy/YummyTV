package su.afk.yummy.tv.feature.details.full

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.details.presentation.R

@HiltViewModel(assistedFactory = FullDetailsViewModel.Factory::class)
class FullDetailsViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val stringProvider: StringProvider,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<FullDetailsState.State, FullDetailsState.Event, FullDetailsState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): FullDetailsViewModel
    }

    override fun createInitialState() = FullDetailsState.State()

    init {
        load()
    }

    override fun onEvent(event: FullDetailsState.Event) {
        when (event) {
            FullDetailsState.Event.BackSelected -> nav.back()
            FullDetailsState.Event.RetrySelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "retry",
                        params = analyticsParamsOf("anime_id" to animeId),
                    )
                )
                load()
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getAnimeDetails(animeId) }.fold(
                onSuccess = { details -> setState { copy(isLoading = false, details = details) } },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message ?: stringProvider.get(R.string.details_load_error),
                        )
                    }
                },
            )
        }
    }
}

private const val SCREEN_NAME = "details_full"

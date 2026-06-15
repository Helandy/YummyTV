package su.afk.yummy.tv.feature.details.screenshots

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

@HiltViewModel(assistedFactory = ScreenshotsViewModel.Factory::class)
class ScreenshotsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<ScreenshotsState.State, ScreenshotsState.Event, ScreenshotsState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): ScreenshotsViewModel
    }

    override fun createInitialState() = ScreenshotsState.State()

    init {
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: ScreenshotsState.Event) {
        when (event) {
            ScreenshotsState.Event.BackSelected -> {
                if (currentState.selectedIndex != null) {
                    setState { copy(selectedIndex = null) }
                } else {
                    nav.back()
                }
            }

            is ScreenshotsState.Event.ScreenshotSelected -> {
                analytics.eventScreenshotsScreenshotSelected(animeId, event.index)
                setState { copy(selectedIndex = event.index) }
            }

            ScreenshotsState.Event.ScreenshotDismissed -> setState { copy(selectedIndex = null) }
            ScreenshotsState.Event.PreviousSelected -> {
                analytics.eventScreenshotsPreviousSelected(animeId)
                setState {
                    copy(selectedIndex = selectedIndex?.let { (it - 1).coerceAtLeast(0) })
                }
            }

            ScreenshotsState.Event.NextSelected -> {
                analytics.eventScreenshotsNextSelected(animeId)
                setState {
                    copy(selectedIndex = selectedIndex?.let { (it + 1).coerceAtMost(screenshots.lastIndex) })
                }
            }
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeDetails(animeId) }.fold(
            onSuccess = { details ->
                setState {
                    copy(
                        isLoading = false,
                        title = details.title,
                        screenshots = details.screenshots,
                    )
                }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = e.message) } },
        )
    }

}

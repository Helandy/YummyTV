package su.afk.yummy.tv.feature.details.screenshots

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
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase

@HiltViewModel(assistedFactory = ScreenshotsViewModel.Factory::class)
class ScreenshotsViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val analyticsTracker: AnalyticsTracker,
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
                trackScreenshotsAction(
                    action = "screenshot_selected",
                    params = analyticsParamsOf("selected_index" to event.index),
                )
                setState { copy(selectedIndex = event.index) }
            }

            ScreenshotsState.Event.ScreenshotDismissed -> setState { copy(selectedIndex = null) }
            ScreenshotsState.Event.PreviousSelected -> {
                trackScreenshotsAction("previous_selected")
                setState {
                    copy(selectedIndex = selectedIndex?.let { (it - 1).coerceAtLeast(0) })
                }
            }

            ScreenshotsState.Event.NextSelected -> {
                trackScreenshotsAction("next_selected")
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

    private fun trackScreenshotsAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ) {
        analyticsTracker.track(
            AnalyticsEvents.uiAction(
                screenName = SCREEN_NAME,
                action = action,
                params = analyticsParamsOf("anime_id" to animeId) + params,
            )
        )
    }
}

private const val SCREEN_NAME = "details_screenshots"

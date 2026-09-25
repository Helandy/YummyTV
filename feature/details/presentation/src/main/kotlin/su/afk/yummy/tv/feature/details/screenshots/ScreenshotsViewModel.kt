package su.afk.yummy.tv.feature.details.screenshots

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
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.details.DetailsAnalytics

@HiltViewModel(assistedFactory = ScreenshotsViewModel.Factory::class)
class ScreenshotsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val analytics: DetailsAnalytics,
    private val imageViewNavigator: IImageViewNavigator,
) : BaseViewModel<ScreenshotsState.State, ScreenshotsState.Event, ScreenshotsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): ScreenshotsViewModel
    }

    override fun createInitialState() = ScreenshotsState.State()

    init {
        analytics.eventScreenshotsScreenOpened(animeId)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: ScreenshotsState.Event) {
        when (event) {
            ScreenshotsState.Event.BackSelected -> nav.back()

            is ScreenshotsState.Event.ScreenshotSelected -> {
                analytics.eventScreenshotsScreenshotSelected(animeId, event.index)
                openScreenshot(event.index)
            }

            ScreenshotsState.Event.RetrySelected -> viewModelScope.launch { load() }
        }
    }

    /** Просмотр всех скриншотов галереей, начиная с выбранного. */
    private fun openScreenshot(index: Int) {
        val urls = currentState.screenshots.mapNotNull { it.full ?: it.small }
        val url = currentState.screenshots.getOrNull(index)?.let { it.full ?: it.small } ?: return
        nav.navigate(
            imageViewNavigator(
                imageUrl = url,
                imageUrls = urls,
                selectedIndex = urls.indexOf(url).coerceAtLeast(0),
            ),
        )
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runSuspendCatching { getAnimeDetails(animeId) }.fold(
            onSuccess = { details ->
                setState {
                    copy(
                        isLoading = false,
                        title = details.title,
                        screenshots = details.screenshots.toImmutableList(),
                    )
                }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = e.userMessage()) } },
        )
    }

}

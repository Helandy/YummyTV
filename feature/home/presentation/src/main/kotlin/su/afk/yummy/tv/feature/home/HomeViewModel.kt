package su.afk.yummy.tv.feature.home

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.watching.handler.ContinueWatchingLaunchHandler
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val stringProvider: StringProvider,
    private val continueWatchingLaunchHandler: ContinueWatchingLaunchHandler,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<HomeState.State, HomeState.Event, HomeState.Effect>(savedStateHandle) {

    override fun createInitialState() = HomeState.State()

    init {
        load()
        watchProgressStore.observeContinueWatching()
            .map { entries ->
                WatchProgressStore.latestByAnime(entries)
            }
            .flowOn(Dispatchers.Default)
            .onEach { inProgress ->
                setState {
                    copy(
                        continueWatching = inProgress,
                        isContinueWatchingLoaded = true,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: HomeState.Event) {
        when (event) {
            is HomeState.Event.AnimeSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "anime_selected",
                        params = analyticsParamsOf("anime_id" to event.seriesId),
                    )
                )
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is HomeState.Event.CollectionSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "collection_selected",
                        params = analyticsParamsOf("collection_id" to event.collectionId),
                    )
                )
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }

            is HomeState.Event.VideoSelected -> Unit
            is HomeState.Event.ContinueWatchingSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "continue_watching_selected",
                        params = analyticsParamsOf(
                            "anime_id" to event.entry.animeId,
                            "video_id" to event.entry.videoId,
                        ),
                    )
                )
                setState {
                    copy(
                        continueWatchingRestoreToken = continueWatchingRestoreToken + 1,
                        focusedItemId = null,
                        focusedSectionId = null,
                    )
                }
                launchContinueWatching(event.entry)
            }

            HomeState.Event.RetrySelected -> {
                analyticsTracker.track(AnalyticsEvents.uiAction(SCREEN_NAME, "retry"))
                load()
            }

            is HomeState.Event.ItemFocused -> onItemFocused(
                event.sectionId,
                event.displayId,
                event.animeId
            )

            HomeState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
        }
    }

    private fun onItemFocused(sectionId: String, displayId: Int, animeId: Int?) {
        if (currentState.focusedSectionId == sectionId && currentState.focusedItemId == displayId) return
        setState {
            copy(
                focusedSectionId = sectionId,
                focusedItemId = displayId,
            )
        }
    }

    private fun setSelectedItemRestoreState(sourceSectionId: String?, displayId: Int?) {
        setState {
            copy(
                focusedSectionId = sourceSectionId ?: focusedSectionId,
                focusedItemId = displayId ?: focusedItemId,
                restoreFocusedItemOnEnter = true,
            )
        }
    }

    private fun launchContinueWatching(entry: WatchProgressEntry) {
        viewModelScope.launch {
            nav.navigate(continueWatchingLaunchHandler.getPlayerDestination(entry))
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getHomeFeed() }.fold(
                onSuccess = { feed ->
                    setState { copy(isLoading = false, feed = feed) }
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message ?: stringProvider.get(R.string.home_load_error)
                        )
                    }
                },
            )
        }
    }
}

private const val SCREEN_NAME = "home"

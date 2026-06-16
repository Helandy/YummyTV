package su.afk.yummy.tv.feature.home

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.watchprogress.ContinueWatchingMerge
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
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
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val stringProvider: StringProvider,
    private val continueWatchingLaunchHandler: ContinueWatchingLaunchHandler,
    private val analytics: HomeAnalytics,
) : BaseViewModelNew<HomeState.State, HomeState.Event, HomeState.Effect>(savedStateHandle) {

    override fun createInitialState() = HomeState.State()

    private var cachedFeedContinueWatching: List<WatchProgressEntry> = emptyList()
    private var localContinueWatching: List<WatchProgressEntry> = emptyList()

    init {
        analytics.eventScreenOpened()
        watchProgressStore.observeContinueWatching()
            .onEach { entries ->
                localContinueWatching = ContinueWatchingMerge.bestByAnime(entries)
                updateContinueWatchingState()
            }
            .launchIn(viewModelScope)
        load()
    }

    override fun onEvent(event: HomeState.Event) {
        when (event) {
            is HomeState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.seriesId)
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is HomeState.Event.CollectionSelected -> {
                analytics.eventCollectionSelected(event.collectionId)
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }

            is HomeState.Event.VideoSelected -> Unit
            is HomeState.Event.ContinueWatchingSelected -> {
                analytics.eventContinueWatchingSelected(event.entry)
                setState {
                    copy(
                        continueWatchingRestoreToken = continueWatchingRestoreToken + 1,
                        continueWatchingRestoreKey = event.entry.continueWatchingFocusKey(),
                        focusedItemId = null,
                        focusedSectionId = null,
                    )
                }
                launchContinueWatching(event.entry)
            }

            HomeState.Event.RetrySelected -> {
                analytics.eventRetry()
                load()
            }

            HomeState.Event.ScreenResumed -> syncCachedContinueWatching()

            HomeState.Event.RefreshRequested -> refresh()

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
        if (!entry.hasPlayableTarget()) {
            nav.navigate(detailsNavigator.getDetailsDest(entry.animeId))
            return
        }
        viewModelScope.launch {
            nav.navigate(continueWatchingLaunchHandler.getPlayerDestination(entry))
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getHomeFeed() }.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
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

    private fun refresh() {
        viewModelScope.launch {
            if (currentState.isLoading && currentState.feed == null) return@launch
            val showInitialLoading = currentState.feed == null
            if (showInitialLoading) {
                setState { copy(isLoading = true, error = null) }
            }
            runCatching { refreshHomeFeed() }.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    if (currentState.feed == null) {
                        setState {
                            copy(
                                isLoading = false,
                                error = e.message ?: stringProvider.get(R.string.home_load_error)
                            )
                        }
                    } else if (showInitialLoading) {
                        setState { copy(isLoading = false) }
                    }
                },
            )
        }
    }

    private fun syncCachedContinueWatching() {
        if (currentState.feed == null) return
        viewModelScope.launch {
            val cachedFeed = runCatching { getCachedHomeFeed() }.getOrNull() ?: return@launch
            val currentFeed = currentState.feed
            applyFeed(
                feed = currentFeed?.copy(continueWatchingItems = cachedFeed.continueWatchingItems)
                    ?: cachedFeed,
                isLoading = false,
            )
        }
    }

    private fun applyFeed(feed: HomeFeed, isLoading: Boolean) {
        cachedFeedContinueWatching = feed.continueWatchingItems.map {
            it.toWatchProgressEntry()
        }
        setState {
            copy(
                isLoading = isLoading,
                feed = feed,
                continueWatching = mergeContinueWatching(),
                isContinueWatchingLoaded = true,
            )
        }
    }

    private fun updateContinueWatchingState() {
        setState {
            copy(
                continueWatching = mergeContinueWatching(),
                isContinueWatchingLoaded = true,
            )
        }
    }

    private fun mergeContinueWatching(): List<WatchProgressEntry> =
        ContinueWatchingMerge.merge(
            feedEntries = cachedFeedContinueWatching,
            localEntries = localContinueWatching,
        )

    private fun HomeContinueWatchingItem.toWatchProgressEntry(): WatchProgressEntry =
        WatchProgressEntry(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
            animeTitle = animeTitle,
            posterUrl = poster?.bestUrl().orEmpty(),
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )

    private fun HomePoster.bestUrl(): String? =
        mega ?: fullsize ?: big ?: medium ?: small

    private fun WatchProgressEntry.continueWatchingFocusKey(): String =
        "$animeId:$episode"

    private fun WatchProgressEntry.hasPlayableTarget(): Boolean =
        videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()
}

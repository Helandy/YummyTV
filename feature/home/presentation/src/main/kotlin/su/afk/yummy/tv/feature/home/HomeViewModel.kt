package su.afk.yummy.tv.feature.home

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.handler.AnimePreviewFocusHandler
import su.afk.yummy.tv.feature.home.handler.ContinueWatchingLaunchHandler
import su.afk.yummy.tv.feature.home.presentation.R
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
    private val animePreviewFocusHandler: AnimePreviewFocusHandler,
    private val continueWatchingLaunchHandler: ContinueWatchingLaunchHandler,
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
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is HomeState.Event.CollectionSelected -> {
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }
            is HomeState.Event.VideoSelected -> Unit
            is HomeState.Event.ContinueWatchingSelected -> {
                setState {
                    copy(
                        continueWatchingRestoreToken = continueWatchingRestoreToken + 1,
                        focusedItemId = null,
                        focusedSectionId = null,
                        focusedPreview = null,
                    )
                }
                launchContinueWatching(event.entry)
            }
            HomeState.Event.RetrySelected -> load()
            is HomeState.Event.ItemFocused -> onItemFocused(
                event.sectionId,
                event.displayId,
                event.animeId
            )
            is HomeState.Event.HeroItemVisible -> prefetchHeroPreviewWindow(event.displayId)
            HomeState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
        }
    }

    private fun onItemFocused(sectionId: String, displayId: Int, animeId: Int?) {
        if (currentState.focusedSectionId == sectionId && currentState.focusedItemId == displayId) return
        animePreviewFocusHandler.cancelFocus()
        prefetchHeroPreviewWindow(displayId)
        if (animeId == null) {
            setState {
                copy(
                    focusedSectionId = sectionId,
                    focusedItemId = displayId,
                    focusedPreview = null,
                )
            }
            return
        }
        setState {
            copy(
                focusedSectionId = sectionId,
                focusedItemId = displayId,
                focusedPreview = null,
            )
        }
        animePreviewFocusHandler.focus(
            scope = viewModelScope,
            animeId = animeId,
            debounceMs = PREVIEW_FOCUS_DEBOUNCE_MS,
            isCurrentFocus = {
                currentState.focusedItemId == displayId &&
                    currentState.focusedSectionId == sectionId
            },
            onCachedPreview = { preview, previews ->
                setState { copy(focusedPreview = preview, animePreviews = previews) }
            },
            onLoadedPreview = { result ->
                if (result.isCurrentFocus) {
                    setState {
                        copy(
                            focusedPreview = result.preview,
                            animePreviews = result.previews
                        )
                    }
                } else {
                    setState { copy(animePreviews = result.previews) }
                }
            }
        )
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

    private fun prefetchHeroPreviewWindow(displayId: Int) {
        val heroItems = currentState.feed?.heroItems.orEmpty()
        val currentIndex = heroItems.indexOfFirst { it.id == displayId }
        if (currentIndex == -1) return
        heroItems.getOrNull(currentIndex)?.animeId?.let(::prefetchPreview)
        heroItems.getOrNull(currentIndex + 1)?.animeId?.let(::prefetchPreview)
    }

    private fun prefetchPreview(animeId: Int) {
        animePreviewFocusHandler.prefetch(viewModelScope, animeId) { previews ->
            setState { copy(animePreviews = previews) }
        }
    }

    private fun launchContinueWatching(entry: WatchProgressEntry) {
        viewModelScope.launch {
            nav.navigate(continueWatchingLaunchHandler.getPlayerDestination(entry))
        }
    }

    private val HomeFeedItem.animeId: Int?
        get() = (action as? HomeFeedItemAction.OpenSeries)?.seriesId

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getHomeFeed() }.fold(
                onSuccess = { feed ->
                    setState { copy(isLoading = false, feed = feed) }
                    prefetchInitialHeroPreviews(feed)
                },
                onFailure = { e ->
                    setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.home_load_error)) }
                },
            )
        }
    }

    private fun prefetchInitialHeroPreviews(feed: HomeFeed) {
        feed.heroItems
            .take(2)
            .mapNotNull { it.animeId }
            .forEach(::prefetchPreview)
    }

    private companion object {
        const val PREVIEW_FOCUS_DEBOUNCE_MS = 250L
    }
}

package su.afk.yummy.tv.feature.library

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.watchprogress.ContinueWatchingMerge
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideoUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.handler.RemoteLibraryLoadResult
import su.afk.yummy.tv.feature.library.handler.RemoteLibrarySyncHandler
import su.afk.yummy.tv.feature.watching.handler.ContinueWatchingLaunchHandler
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val libraryStore: LibraryStore,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val removeWatchedVideo: RemoveWatchedVideoUseCase,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val remoteLibrarySyncHandler: RemoteLibrarySyncHandler,
    private val continueWatchingLaunchHandler: ContinueWatchingLaunchHandler,
    private val analytics: LibraryAnalytics,
) : BaseViewModelNew<LibraryState.State, LibraryState.Event, LibraryState.Effect>(savedStateHandle) {

    override fun createInitialState() = LibraryState.State(
        selectedTab = savedStateHandle.get<String>(KEY_SELECTED_TAB)
            ?.let { runCatching { LibraryTab.valueOf(it) }.getOrNull() }
            ?: LibraryTab.CONTINUE_WATCHING,
        focusedItemId = savedStateHandle.get<Int>(KEY_FOCUSED_ITEM_ID),
    )

    override fun saveToSavedState(state: LibraryState.State) {
        savedStateHandle[KEY_SELECTED_TAB] = state.selectedTab.name
        state.focusedItemId?.let { savedStateHandle[KEY_FOCUSED_ITEM_ID] = it }
            ?: savedStateHandle.remove<Int>(KEY_FOCUSED_ITEM_ID)
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selectedTab"
        const val KEY_FOCUSED_ITEM_ID = "focusedItemId"
    }

    private var remoteListsJob: Job? = null
    private var signedInUserId: Int = 0
    private var cachedFeedContinueWatching: List<WatchProgressEntry> = emptyList()
    private var localContinueWatching: List<WatchProgressEntry> = emptyList()
    private var localContinueWatchingEntries: List<WatchProgressEntry> = emptyList()

    init {
        libraryStore.observeAll()
            .onEach { entries -> setState { copy(items = entries) } }
            .launchIn(viewModelScope)

        watchProgressStore.observeContinueWatching()
            .onEach { entries ->
                localContinueWatchingEntries = entries
                localContinueWatching = ContinueWatchingMerge.bestByAnime(entries)
                updateContinueWatchingState()
            }
            .launchIn(viewModelScope)
        loadCachedContinueWatching()
        settingsStore.yaniUserId
            .onEach { userId ->
                signedInUserId = userId
                if (userId > 0) {
                    setState { copy(isSignedIn = true) }
                    loadRemoteLists(userId)
                } else {
                    remoteListsJob?.cancel()
                    setState {
                        copy(
                            isSignedIn = false,
                            isRemoteLoading = false,
                            remoteError = null,
                            remoteItems = emptyMap(),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: LibraryState.Event) {
        when (event) {
            is LibraryState.Event.AnimeSelected -> {
                analytics.eventLocalAnimeSelected(event.animeId, currentState.selectedTab)
                openDetails(event.animeId)
            }

            is LibraryState.Event.RemoteAnimeSelected -> {
                analytics.eventRemoteAnimeSelected(event.animeId, currentState.selectedTab)
                openDetails(event.animeId)
            }

            is LibraryState.Event.ContinueWatchingSelected -> {
                analytics.eventContinueWatchingSelected(event.entry)
                launchContinueWatching(event.entry)
            }

            is LibraryState.Event.ItemFocused ->
                onItemFocused(event.animeId)

            is LibraryState.Event.TabSelected -> {
                if (event.tab != currentState.selectedTab) {
                    analytics.eventTabSelected(event.tab)
                    setState {
                        copy(
                            selectedTab = event.tab,
                            focusedItemId = null,
                        )
                    }
                }
            }

            LibraryState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }

            LibraryState.Event.ScreenResumed -> {
                refreshRemoteLists()
                loadCachedContinueWatching()
            }

            LibraryState.Event.RetrySelected -> {
                analytics.eventRetry()
                refreshRemoteLists()
            }

            is LibraryState.Event.RemoveLibraryEntry ->
                viewModelScope.launch {
                    analytics.eventRemoveLibraryEntry(event.animeId)
                    libraryStore.remove(event.animeId)
                    setEffect(LibraryState.Effect.ItemRemoved)
                }

            is LibraryState.Event.RemoveFavoriteEntry ->
                viewModelScope.launch {
                    analytics.eventRemoveFavoriteEntry(event.animeId)
                    libraryStore.setFavorite(
                        event.animeId,
                        title = "",
                        poster = null,
                        favorite = false
                    )
                    setEffect(LibraryState.Effect.ItemRemoved)
                }

            is LibraryState.Event.RemoveWatchProgress ->
                viewModelScope.launch {
                    analytics.eventRemoveWatchProgress(event.animeId)
                    val remoteVideoIds = if (signedInUserId > 0) {
                        knownContinueWatchingEntriesFor(event.animeId)
                            .mapNotNull { it.videoId.takeIf { id -> id > 0 } }
                            .distinct()
                    } else {
                        emptyList()
                    }
                    watchProgressStore.deleteByAnimeId(event.animeId)
                    removeCachedContinueWatching(event.animeId)
                    cachedFeedContinueWatching =
                        cachedFeedContinueWatching.filterNot { it.animeId == event.animeId }
                    localContinueWatching =
                        localContinueWatching.filterNot { it.animeId == event.animeId }
                    localContinueWatchingEntries =
                        localContinueWatchingEntries.filterNot { it.animeId == event.animeId }
                    updateContinueWatchingState()
                    remoteVideoIds.forEach { videoId ->
                        runCatching { removeWatchedVideo(videoId) }
                    }
                    setEffect(LibraryState.Effect.ItemRemoved)
                }

            is LibraryState.Event.RemoveRemoteEntry -> removeRemoteEntry(event)
        }
    }

    private fun loadCachedContinueWatching() {
        viewModelScope.launch {
            cachedFeedContinueWatching = runCatching {
                getCachedHomeFeed()
                    ?.continueWatchingItems
                    .orEmpty()
                    .map { it.toWatchProgressEntry() }
            }.getOrDefault(emptyList())
            updateContinueWatchingState()
        }
    }

    private fun updateContinueWatchingState() {
        setState {
            copy(
                continueWatching = mergeContinueWatching(
                    cached = cachedFeedContinueWatching,
                    local = localContinueWatching,
                )
            )
        }
    }

    private fun mergeContinueWatching(
        cached: List<WatchProgressEntry>,
        local: List<WatchProgressEntry>,
    ): List<WatchProgressEntry> =
        ContinueWatchingMerge.merge(
            feedEntries = cached,
            localEntries = local,
        )

    private fun knownContinueWatchingEntriesFor(animeId: Int): List<WatchProgressEntry> =
        (
                currentState.continueWatching +
                        cachedFeedContinueWatching +
                        localContinueWatching +
                        localContinueWatchingEntries
                )
            .filter { it.animeId == animeId }

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

    private fun openDetails(animeId: Int) {
        setState {
            copy(
                focusedItemId = animeId,
                restoreFocusedItemOnEnter = true
            )
        }
        nav.navigate(detailsNavigator.getDetailsDest(animeId))
    }

    private fun refreshRemoteLists() {
        if (signedInUserId > 0) loadRemoteLists(signedInUserId)
    }

    private fun loadRemoteLists(userId: Int) {
        remoteListsJob?.cancel()
        remoteListsJob = viewModelScope.launch {
            setState { copy(isRemoteLoading = true, remoteError = null) }
            when (val result = remoteLibrarySyncHandler.loadRemoteLists(userId)) {
                is RemoteLibraryLoadResult.Success -> {
                    setState {
                        copy(
                            remoteItems = result.remoteItems,
                            remoteError = result.syncError,
                            isRemoteLoading = false,
                        )
                    }
                }

                is RemoteLibraryLoadResult.Failure -> {
                    setState {
                        copy(
                            remoteError = result.message,
                            isRemoteLoading = false,
                        )
                    }
                }
            }
        }
    }

    private fun removeRemoteEntry(event: LibraryState.Event.RemoveRemoteEntry) {
        analytics.eventRemoveRemoteEntry(
            animeId = event.animeId,
            tab = currentState.selectedTab,
            favorite = event.favorite,
            list = event.list,
        )
        val previous = currentState.remoteItems
        val tab = currentState.selectedTab
        setState {
            copy(
                remoteItems = remoteItems + (tab to remoteItems[tab].orEmpty()
                    .filterNot { it.animeId == event.animeId })
            )
        }
        viewModelScope.launch {
            val result = remoteLibrarySyncHandler.removeRemoteEntry(
                animeId = event.animeId,
                favorite = event.favorite,
            )
            if (result.isFailure) {
                setState {
                    copy(
                        remoteItems = previous,
                        remoteError = result.exceptionOrNull()?.message
                    )
                }
            } else {
                setEffect(LibraryState.Effect.ItemRemoved)
            }
        }
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        setState { copy(focusedItemId = animeId) }
    }

    private fun launchContinueWatching(entry: WatchProgressEntry) {
        viewModelScope.launch {
            nav.navigate(continueWatchingLaunchHandler.getPlayerDestination(entry))
        }
    }

}

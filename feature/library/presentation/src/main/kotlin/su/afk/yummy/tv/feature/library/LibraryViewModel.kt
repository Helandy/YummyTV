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
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetContinueWatchingVideoIdsUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.handler.RemoteLibraryLoadResult
import su.afk.yummy.tv.feature.library.handler.RemoteLibrarySyncHandler
import su.afk.yummy.tv.feature.library.handler.RemoteWatchProgressRemovalHandler
import su.afk.yummy.tv.feature.library.utils.userAnimeList
import su.afk.yummy.tv.feature.watching.handler.ContinueWatchingLaunchHandler
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val libraryStore: LibraryStore,
    private val settingsStore: SettingsStore,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val getContinueWatchingVideoIds: GetContinueWatchingVideoIdsUseCase,
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val remoteLibrarySyncHandler: RemoteLibrarySyncHandler,
    private val remoteWatchProgressRemovalHandler: RemoteWatchProgressRemovalHandler,
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

    init {
        analytics.eventScreenOpened()
        libraryStore.observeAll()
            .onEach { entries -> setState { copy(items = entries) } }
            .launchIn(viewModelScope)

        observeContinueWatching()
            .onEach { items ->
                setState { copy(continueWatching = items) }
            }
            .launchIn(viewModelScope)
        loadCachedContinueWatching()
        settingsStore.yaniUserId
            .onEach { userId ->
                signedInUserId = userId
                if (userId > 0) {
                    setState { copy(isSignedIn = true) }
                    loadRemoteLists(userId, forceRefresh = true)
                } else {
                    remoteListsJob?.cancel()
                    setState {
                        copy(
                            isSignedIn = false,
                            isRemoteLoading = false,
                            remoteError = null,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: LibraryState.Event) {
        when (event) {
            is LibraryState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.animeId, currentState.selectedTab)
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
                refreshRemoteLists(forceRefresh = true)
                loadCachedContinueWatching()
            }

            LibraryState.Event.RetrySelected -> {
                analytics.eventRetry()
                refreshRemoteLists(forceRefresh = true)
            }

            is LibraryState.Event.RemoveEntry -> removeEntry(event)

            is LibraryState.Event.RemoveWatchProgress ->
                removeWatchProgress(event.animeId)

        }
    }

    private fun removeWatchProgress(animeId: Int) {
        analytics.eventRemoveWatchProgress(animeId)
        viewModelScope.launch {
            val knownRemoteVideoIds = knownRemoteVideoIdsFor(animeId)
            suppressContinueWatchingLocally(animeId)
            setEffect(LibraryState.Effect.ItemRemoved)
            removeRemoteWatchProgress(animeId, knownRemoteVideoIds)
        }
    }

    private fun loadCachedContinueWatching() {
        viewModelScope.launch {
            runCatching { getCachedHomeFeed() }
        }
    }

    private suspend fun knownRemoteVideoIdsFor(animeId: Int): List<Int> =
        if (signedInUserId > 0) {
            getContinueWatchingVideoIds(animeId)
        } else {
            emptyList()
        }

    private suspend fun suppressContinueWatchingLocally(animeId: Int) {
        removeCachedContinueWatching(animeId)
    }

    private suspend fun removeRemoteWatchProgress(
        animeId: Int,
        knownVideoIds: List<Int>,
    ) {
        if (signedInUserId <= 0) return
        remoteWatchProgressRemovalHandler.removeAnimeWatchProgress(
            animeId = animeId,
            knownVideoIds = knownVideoIds,
        )
    }

    private fun openDetails(animeId: Int) {
        setState {
            copy(
                focusedItemId = animeId,
                restoreFocusedItemOnEnter = true
            )
        }
        nav.navigate(detailsNavigator.getDetailsDest(animeId))
    }

    private fun refreshRemoteLists(forceRefresh: Boolean = false) {
        if (signedInUserId > 0) loadRemoteLists(signedInUserId, forceRefresh)
    }

    private fun loadRemoteLists(
        userId: Int,
        forceRefresh: Boolean = false,
    ) {
        remoteListsJob?.cancel()
        remoteListsJob = viewModelScope.launch {
            setState { copy(isRemoteLoading = true, remoteError = null) }
            when (val result = remoteLibrarySyncHandler.loadRemoteLists(userId, forceRefresh)) {
                is RemoteLibraryLoadResult.Success -> {
                    result.syncError?.let { analytics.eventLoadError(it) }
                    setState {
                        copy(
                            remoteError = result.syncError?.message,
                            isRemoteLoading = false,
                        )
                    }
                }

                is RemoteLibraryLoadResult.Failure -> {
                    analytics.eventLoadError(result.error)
                    setState {
                        copy(
                            remoteError = result.error.message,
                            isRemoteLoading = false,
                        )
                    }
                }
            }
        }
    }

    private fun removeEntry(event: LibraryState.Event.RemoveEntry) {
        val shouldRemoveRemote = signedInUserId > 0
        val selectedTab = currentState.selectedTab
        analytics.eventRemoveEntry(
            animeId = event.animeId,
            tab = selectedTab,
            target = event.target,
            remote = shouldRemoveRemote,
            list = if (shouldRemoveRemote && event.target == LibraryRemoveTarget.LIST) {
                selectedTab.userAnimeList()
            } else {
                null
            },
        )
        viewModelScope.launch {
            if (shouldRemoveRemote) {
                remoteLibrarySyncHandler.removeRemoteEntry(event.animeId, event.target)
                    .onSuccess { removeLocalEntry(event) }
                    .onFailure { error ->
                        analytics.eventRemoveError(event.target, error)
                        setState { copy(remoteError = error.message) }
                    }
            } else {
                removeLocalEntry(event)
            }
        }
    }

    private suspend fun removeLocalEntry(event: LibraryState.Event.RemoveEntry) {
        when (event.target) {
            LibraryRemoveTarget.LIST -> libraryStore.remove(event.animeId)
            LibraryRemoveTarget.FAVORITE -> libraryStore.setFavorite(
                event.animeId,
                title = "",
                poster = null,
                favorite = false,
            )
        }
        setEffect(LibraryState.Effect.ItemRemoved)
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        setState { copy(focusedItemId = animeId) }
    }

    private fun launchContinueWatching(entry: HomeContinueWatchingItem) {
        viewModelScope.launch {
            nav.navigate(continueWatchingLaunchHandler.getPlayerDestination(entry))
        }
    }

}

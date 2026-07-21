package su.afk.yummy.tv.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.domain.library.usecase.GetWatchHistoryPageUseCase
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryItemsUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoteLibrarySyncResult
import su.afk.yummy.tv.domain.library.usecase.RemoveLibraryItemUseCase
import su.afk.yummy.tv.domain.library.usecase.SetLibraryFavoriteUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.handler.HistoryLaunchHandler
import su.afk.yummy.tv.feature.library.handler.RemoteLibrarySyncHandler
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.utils.toToastTimeString
import su.afk.yummy.tv.feature.library.utils.userAnimeList
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.getPlayerDest
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val observeLibraryItems: ObserveLibraryItemsUseCase,
    private val removeLibraryItem: RemoveLibraryItemUseCase,
    private val setLibraryFavorite: SetLibraryFavoriteUseCase,
    private val settingsStore: SettingsStore,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val remoteLibrarySyncHandler: RemoteLibrarySyncHandler,
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase,
    private val playerNavigator: IPlayerNavigator,
    private val getWatchHistoryPage: GetWatchHistoryPageUseCase,
    private val historyLaunchHandler: HistoryLaunchHandler,
    private val stringProvider: StringProvider,
    private val analytics: LibraryAnalytics,
) : BaseViewModelNew<LibraryState.State, LibraryState.Event, LibraryState.Effect>(savedStateHandle) {

    override fun createInitialState() = LibraryState.State(
        watchHistory = createWatchHistoryFlow(),
        selectedTab = savedStateHandle.get<String>(KEY_SELECTED_TAB)
            ?.let { runCatching { LibraryTab.valueOf(it) }.getOrNull() }
            ?.takeIf { it in LibraryTab.visibleEntries }
            ?: LibraryTab.CONTINUE_WATCHING
    )

    override fun saveToSavedState(state: LibraryState.State) {
        savedStateHandle[KEY_SELECTED_TAB] = state.selectedTab.name
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selectedTab"
    }

    private var remoteListsJob: Job? = null
    private var signedInUserId: Int = 0

    init {
        analytics.eventScreenOpened()
        observeLibraryItems()
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
        settingsStore.libraryContinueWatchingCardSize
            .onEach { size ->
                setState { copy(continueWatchingCardSize = size) }
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

            is LibraryState.Event.ContinueWatchingDetailsSelected -> {
                analytics.eventContinueWatchingDetailsSelected(event.entry)
                openDetails(event.entry.animeId)
            }

            is LibraryState.Event.HistorySelected -> launchHistory(event.entry)

            is LibraryState.Event.HistoryDetailsSelected -> openDetails(event.animeId)

            is LibraryState.Event.TabSelected -> {
                if (
                    event.tab in LibraryTab.visibleEntries &&
                    event.tab != currentState.selectedTab
                ) {
                    analytics.eventTabSelected(event.tab)
                    setState { copy(selectedTab = event.tab) }
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
                removeWatchProgress(event.entry)

        }
    }

    private fun removeWatchProgress(entry: HomeContinueWatchingItem) {
        val animeId = entry.animeId
        analytics.eventRemoveWatchProgress(animeId)
        viewModelScope.launch {
            suppressContinueWatchingLocally(animeId)
            setEffect(LibraryState.Effect.ItemRemoved)
        }
    }

    private fun loadCachedContinueWatching() {
        viewModelScope.launch {
            runCatching { getCachedHomeFeed() }
        }
    }

    private suspend fun suppressContinueWatchingLocally(animeId: Int) {
        removeCachedContinueWatching(animeId)
    }

    private fun openDetails(animeId: Int) {
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
                is RemoteLibrarySyncResult.Success -> {
                    result.syncError?.let { analytics.eventLoadError(it) }
                    setState {
                        copy(
                            remoteError = result.syncError?.message,
                            isRemoteLoading = false,
                        )
                    }
                }

                is RemoteLibrarySyncResult.Failure -> {
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
            LibraryRemoveTarget.LIST -> removeLibraryItem(event.animeId)
            LibraryRemoveTarget.FAVORITE -> setLibraryFavorite(
                event.animeId,
                title = "",
                poster = null,
                favorite = false,
            )
        }
        setEffect(LibraryState.Effect.ItemRemoved)
    }

    private fun launchContinueWatching(entry: HomeContinueWatchingItem) {
        viewModelScope.launch {
            val result = resolveContinueWatchingLaunch(
                entry = entry,
                refreshProgressOnLaunch = settingsStore
                    .refreshContinueWatchingProgressOnLaunch
                    .first(),
            )
            result.remoteProgressSwitch?.let { progress ->
                setEffect(
                    LibraryState.Effect.ShowToast(
                        stringProvider.get(
                            R.string.library_remote_continue_progress_toast,
                            progress.episode,
                            progress.positionMs.toToastTimeString(),
                        )
                    )
                )
            }
            nav.navigate(playerNavigator.getPlayerDest(result))
        }
    }

    private fun launchHistory(entry: su.afk.yummy.tv.domain.library.model.WatchHistoryEntry) {
        viewModelScope.launch {
            runCatching { historyLaunchHandler.destination(entry) }
                .onSuccess(nav::navigate)
                .onFailure {
                    setEffect(
                        LibraryState.Effect.ShowToast(
                            it.message ?: stringProvider.get(R.string.library_history_launch_error),
                        ),
                    )
                }
        }
    }

    private fun createWatchHistoryFlow() = Pager(
        config = PagingConfig(pageSize = 100, initialLoadSize = 100, enablePlaceholders = false),
        pagingSourceFactory = {
            OffsetPagingSource { limit, offset ->
                val items = getWatchHistoryPage(limit.coerceAtMost(100), offset)
                OffsetPage(
                    items = items,
                    nextOffset = offset + items.size,
                    canLoadMore = items.size >= limit.coerceAtMost(100),
                )
            }
        },
    ).flow.cachedIn(viewModelScope)

}

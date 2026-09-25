package su.afk.yummy.tv.feature.library

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.domain.library.usecase.GetWatchHistoryPageUseCase
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryItemsUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoteLibrarySyncResult
import su.afk.yummy.tv.domain.library.usecase.RemoveLibraryItemUseCase
import su.afk.yummy.tv.domain.library.usecase.SetLibraryFavoriteUseCase
import su.afk.yummy.tv.domain.player.usecase.GetMeaningfulVideoProgressUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.handler.RemoteLibrarySyncHandler
import su.afk.yummy.tv.feature.library.model.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.feature.library.utils.buildLibraryTabItems
import su.afk.yummy.tv.feature.library.utils.historyProgressKey
import su.afk.yummy.tv.feature.library.utils.toToastTimeString
import su.afk.yummy.tv.feature.library.utils.userAnimeList
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.getPlayerDest
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject internal constructor(
    private val savedStateHandle: SavedStateHandle,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val observeLibraryItems: ObserveLibraryItemsUseCase,
    private val removeLibraryItem: RemoveLibraryItemUseCase,
    private val setLibraryFavorite: SetLibraryFavoriteUseCase,
    private val settingsStore: SettingsStore,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val remoteLibrarySyncHandler: RemoteLibrarySyncHandler,
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase,
    private val playerNavigator: IPlayerNavigator,
    private val getWatchHistoryPage: GetWatchHistoryPageUseCase,
    private val getMeaningfulVideoProgress: GetMeaningfulVideoProgressUseCase,
    private val stringProvider: StringProvider,
    private val analytics: LibraryAnalytics,
) : BaseViewModel<LibraryState.State, LibraryState.Event, LibraryState.Effect>() {

    override fun createInitialState() = LibraryState.State(
        watchHistory = createWatchHistoryFlow(),
        selectedTab = savedStateHandle.get<String>(KEY_SELECTED_TAB)
            ?.let { runCatching { LibraryTab.valueOf(it) }.getOrNull() }
            ?.takeIf { it in LibraryTab.visibleEntries }
            ?: LibraryTab.CONTINUE_WATCHING,
    )

    private companion object {
        const val KEY_SELECTED_TAB = "selectedTab"
    }

    private var remoteListsJob: Job? = null
    private var signedInUserId: Int = 0

    init {
        analytics.eventScreenOpened()
        observeLibraryItems()
            .onEach { entries ->
                setState {
                    copy(
                        items = entries.toImmutableList(),
                        tabItems = buildLibraryTabItems(entries, sort, sortDirection),
                    )
                }
            }
            .launchIn(viewModelScope)

        observeContinueWatching()
            .onEach { items ->
                setState { copy(continueWatching = items.toImmutableList()) }
            }
            .launchIn(viewModelScope)
        loadCachedContinueWatching()
        loadHistoryLocalProgress()
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
        settingsStore.showLibraryTitleYear
            .onEach { enabled ->
                setState { copy(showTitleYear = enabled) }
            }
            .launchIn(viewModelScope)
        settingsStore.librarySort
            .onEach { sort ->
                setState {
                    copy(
                        sort = sort,
                        tabItems = buildLibraryTabItems(items, sort, sortDirection),
                    )
                }
            }
            .launchIn(viewModelScope)
        settingsStore.librarySortDirection
            .onEach { direction ->
                setState {
                    copy(
                        sortDirection = direction,
                        tabItems = buildLibraryTabItems(items, sort, direction),
                    )
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
                    savedStateHandle[KEY_SELECTED_TAB] = event.tab.name
                }
            }

            is LibraryState.Event.SortSelected -> {
                if (event.sort != currentState.sort) {
                    analytics.eventSortSelected(event.sort, currentState.sortDirection)
                    viewModelScope.launch { settingsStore.setLibrarySort(event.sort) }
                }
            }

            LibraryState.Event.SortDirectionToggled -> {
                val direction = when (currentState.sortDirection) {
                    LibrarySortDirection.DESC -> LibrarySortDirection.ASC
                    LibrarySortDirection.ASC -> LibrarySortDirection.DESC
                }
                analytics.eventSortSelected(currentState.sort, direction)
                viewModelScope.launch { settingsStore.setLibrarySortDirection(direction) }
            }

            LibraryState.Event.ScreenResumed -> {
                refreshRemoteLists(forceRefresh = true)
                loadCachedContinueWatching()
                loadHistoryLocalProgress()
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
            runSuspendCatching { getCachedHomeFeed() }
        }
    }

    private suspend fun suppressContinueWatchingLocally(animeId: Int) {
        removeCachedContinueWatching(animeId)
    }

    private fun loadHistoryLocalProgress() {
        viewModelScope.launch {
            val lookup = getMeaningfulVideoProgress()
                .associateBy { it.historyProgressKey }
                .toImmutableMap()
            setState { copy(historyLocalProgress = lookup) }
        }
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
                            remoteError = result.syncError?.userMessage(),
                            isRemoteLoading = false,
                        )
                    }
                }

                is RemoteLibrarySyncResult.Failure -> {
                    analytics.eventLoadError(result.error)
                    setState {
                        copy(
                            remoteError = result.error.userMessage(),
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
                        setState { copy(remoteError = error.userMessage()) }
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
                year = null,
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
                        ),
                    ),
                )
            }
            nav.navigate(playerNavigator.getPlayerDest(result))
        }
    }

    private fun launchHistory(entry: WatchHistoryEntry) {
        nav.navigate(detailsNavigator.getEpisodesDest(entry.animeId, entry.episode))
    }

    private fun createWatchHistoryFlow() =
        pagingFlow(
            viewModelScope,
            pageSize = 100,
            // Пересмотр и разные озвучки дают повторы animeId+серия, поэтому в ключ входит
            // время просмотра: дедуплицируется ровно «сервер отдал запись на двух страницах».
            itemKey = { "${it.animeId}:${it.episode}:${it.watchedAtSeconds}" },
        ) { limit, offset ->
            getWatchHistoryPage(limit, offset)
        }
}

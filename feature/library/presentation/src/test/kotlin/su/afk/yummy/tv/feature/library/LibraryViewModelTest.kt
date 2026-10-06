package su.afk.yummy.tv.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.RemoteLibrarySyncResult
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.domain.library.usecase.GetWatchHistoryPageUseCase
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryItemsUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoveLibraryItemUseCase
import su.afk.yummy.tv.domain.library.usecase.SetLibraryFavoriteUseCase
import su.afk.yummy.tv.domain.player.usecase.GetMeaningfulVideoProgressUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.LibraryState.Effect
import su.afk.yummy.tv.feature.library.LibraryState.Event
import su.afk.yummy.tv.feature.library.handler.RemoteLibrarySyncHandler
import su.afk.yummy.tv.feature.library.model.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.player.IPlayerNavigator

class LibraryViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val observeLibraryItems: ObserveLibraryItemsUseCase = mockk()
    private val removeLibraryItem: RemoveLibraryItemUseCase = mockk(relaxed = true)
    private val setLibraryFavorite: SetLibraryFavoriteUseCase = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase = mockk()
    private val observeContinueWatching: ObserveContinueWatchingUseCase = mockk()
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val remoteLibrarySyncHandler: RemoteLibrarySyncHandler = mockk()
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase = mockk()
    private val playerNavigator: IPlayerNavigator = mockk()
    private val getWatchHistoryPage: GetWatchHistoryPageUseCase = mockk()
    private val getMeaningfulVideoProgress: GetMeaningfulVideoProgressUseCase = mockk()
    private val stringProvider: StringProvider = mockk()
    private val analytics: LibraryAnalytics = mockk(relaxed = true)

    private val savedStateHandle = SavedStateHandle()
    private val items = MutableStateFlow<List<LibraryItem>>(emptyList())
    private val continueWatching = MutableStateFlow<List<HomeContinueWatchingItem>>(emptyList())
    private val userId = MutableStateFlow(USER_ID)
    private val sort = MutableStateFlow(LibrarySort.entries.first())
    private val sortDirection = MutableStateFlow(LibrarySortDirection.DESC)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { observeLibraryItems() } returns items
        every { observeContinueWatching() } returns continueWatching
        every { settingsStore.yaniUserId } returns userId
        every { settingsStore.libraryContinueWatchingCardSize } returns flowOf(LibraryContinueWatchingCardSize.entries.first())
        every { settingsStore.showLibraryTitleYear } returns flowOf(true)
        every { settingsStore.librarySort } returns sort
        every { settingsStore.librarySortDirection } returns sortDirection
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { detailsNavigator.getEpisodesDest(any(), any()) } returns navKey
        coEvery { getCachedHomeFeed() } returns null
        coEvery { getMeaningfulVideoProgress() } returns emptyList()
        coEvery { remoteLibrarySyncHandler.loadRemoteLists(any(), any()) } returns RemoteLibrarySyncResult.Success()
    }

    private fun createViewModel() = LibraryViewModel(
        savedStateHandle = savedStateHandle,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        observeLibraryItems = observeLibraryItems,
        removeLibraryItem = removeLibraryItem,
        setLibraryFavorite = setLibraryFavorite,
        settingsStore = settingsStore,
        getCachedHomeFeed = getCachedHomeFeed,
        observeContinueWatching = observeContinueWatching,
        removeCachedContinueWatching = removeCachedContinueWatching,
        nav = nav,
        detailsNavigator = detailsNavigator,
        remoteLibrarySyncHandler = remoteLibrarySyncHandler,
        resolveContinueWatchingLaunch = resolveContinueWatchingLaunch,
        playerNavigator = playerNavigator,
        getWatchHistoryPage = getWatchHistoryPage,
        getMeaningfulVideoProgress = getMeaningfulVideoProgress,
        stringProvider = stringProvider,
        analytics = analytics,
    )

    private fun watching() = HomeContinueWatchingItem(
        animeId = 5,
        animeTitle = "Anime",
        description = "",
        poster = null,
        videoId = 1,
        episode = "1",
        episodeUrl = "",
        positionMs = 0L,
        durationMs = 0L,
        updatedAt = 0L,
        playerName = "",
        dubbing = "",
        screenshotUrl = "",
    )

    @Test
    fun `starts on continue watching and mirrors the settings`() {
        val state = createViewModel().currentState

        assertEquals(LibraryTab.CONTINUE_WATCHING, state.selectedTab)
        assertTrue(state.showTitleYear)
        assertEquals(LibrarySortDirection.DESC, state.sortDirection)
        verify(exactly = 1) { analytics.eventScreenOpened() }
    }

    @Test
    fun `library items and continue watching are mirrored`() {
        val vm = createViewModel()

        items.value = listOf(LibraryItem(animeId = 1, title = "A"))
        continueWatching.value = listOf(watching())

        assertEquals(listOf(1), vm.currentState.items.map { it.animeId })
        assertEquals(1, vm.currentState.continueWatching.size)
    }

    @Test
    fun `saved tab is restored`() {
        savedStateHandle["selectedTab"] = LibraryTab.FAVORITES.name

        assertEquals(LibraryTab.FAVORITES, createViewModel().currentState.selectedTab)
    }

    @Test
    fun `tab selection is stored and tracked once`() {
        val vm = createViewModel()

        vm.setEvent(Event.TabSelected(LibraryTab.PLANNED))
        vm.setEvent(Event.TabSelected(LibraryTab.PLANNED))

        assertEquals(LibraryTab.PLANNED, vm.currentState.selectedTab)
        assertEquals(LibraryTab.PLANNED.name, savedStateHandle.get<String>("selectedTab"))
        verify(exactly = 1) { analytics.eventTabSelected(LibraryTab.PLANNED) }
    }

    @Test
    fun `signed-in user loads the remote lists`() {
        val state = createViewModel().currentState

        assertTrue(state.isSignedIn)
        assertFalse(state.isRemoteLoading)
        assertNull(state.remoteError)
        coVerify(exactly = 1) { remoteLibrarySyncHandler.loadRemoteLists(USER_ID, true) }
    }

    @Test
    fun `guest does not load the remote lists`() {
        userId.value = 0

        val state = createViewModel().currentState

        assertFalse(state.isSignedIn)
        coVerify(exactly = 0) { remoteLibrarySyncHandler.loadRemoteLists(any(), any()) }
    }

    @Test
    fun `failed remote sync shows the error and retry loads again`() {
        coEvery { remoteLibrarySyncHandler.loadRemoteLists(any(), any()) } returns
            RemoteLibrarySyncResult.Failure(IllegalStateException("offline"))
        val vm = createViewModel()
        assertEquals("offline", vm.currentState.remoteError)
        coEvery { remoteLibrarySyncHandler.loadRemoteLists(any(), any()) } returns RemoteLibrarySyncResult.Success()

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.remoteError)
        verify(exactly = 1) { analytics.eventRetry() }
    }

    @Test
    fun `sorting changes are written to the settings`() {
        val vm = createViewModel()
        val other = LibrarySort.entries.last()

        vm.setEvent(Event.SortSelected(other))
        vm.setEvent(Event.SortSelected(sort.value))
        vm.setEvent(Event.SortDirectionToggled)

        coVerify(exactly = 1) { settingsStore.setLibrarySort(other) }
        coVerify(exactly = 1) { settingsStore.setLibrarySortDirection(LibrarySortDirection.DESC.toggled()) }
    }

    @Test
    fun `signed-in removal goes to the server first and then to the local list`() = runTest {
        coEvery { remoteLibrarySyncHandler.removeRemoteEntry(7, LibraryRemoveTarget.LIST) } returns Result.success(Unit)
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RemoveEntry(7, LibraryRemoveTarget.LIST))

        coVerify(exactly = 1) { removeLibraryItem(7) }
        assertEquals(listOf<Effect>(Effect.ItemRemoved), effects)
    }

    @Test
    fun `failed server removal keeps the local entry and shows the error`() = runTest {
        coEvery { remoteLibrarySyncHandler.removeRemoteEntry(any(), any()) } returns
            Result.failure(IllegalStateException("denied"))
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RemoveEntry(7, LibraryRemoveTarget.LIST))

        coVerify(exactly = 0) { removeLibraryItem(any()) }
        assertEquals("denied", vm.currentState.remoteError)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `guest removal only touches the local data`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RemoveEntry(7, LibraryRemoveTarget.FAVORITE))

        coVerify(exactly = 1) { setLibraryFavorite(7, any(), any(), any(), false) }
        coVerify(exactly = 0) { remoteLibrarySyncHandler.removeRemoteEntry(any(), any()) }
        assertEquals(listOf<Effect>(Effect.ItemRemoved), effects)
    }

    @Test
    fun `removing watch progress hides the cached entry`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RemoveWatchProgress(watching()))

        coVerify(exactly = 1) { removeCachedContinueWatching(5) }
        assertEquals(listOf<Effect>(Effect.ItemRemoved), effects)
    }

    @Test
    fun `anime history and details selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(3))
        vm.setEvent(Event.HistoryDetailsSelected(4))
        vm.setEvent(Event.ContinueWatchingDetailsSelected(watching()))
        vm.setEvent(
            Event.HistorySelected(
                WatchHistoryEntry(
                    animeId = 9,
                    animeUrl = "",
                    title = "",
                    episode = "2",
                    posterUrl = null,
                    screenshotUrl = null,
                    watchedAtSeconds = 0L,
                    positionSeconds = 0,
                    durationSeconds = 0,
                ),
            ),
        )

        verify(exactly = 1) { detailsNavigator.getDetailsDest(3) }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(4) }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(5) }
        verify(exactly = 1) { detailsNavigator.getEpisodesDest(9, "2") }
        verify(exactly = 4) { nav.navigate(navKey) }
    }

    @Test
    fun `resume refreshes the remote lists and local progress`() {
        val vm = createViewModel()

        vm.setEvent(Event.ScreenResumed)

        coVerify(exactly = 2) { remoteLibrarySyncHandler.loadRemoteLists(USER_ID, true) }
        coVerify(exactly = 2) { getMeaningfulVideoProgress() }
    }

    private companion object {
        const val USER_ID = 5
    }
}

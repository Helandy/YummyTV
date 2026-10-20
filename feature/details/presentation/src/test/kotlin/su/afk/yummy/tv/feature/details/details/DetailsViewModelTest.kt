package su.afk.yummy.tv.feature.details.details

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.AnimeLibraryState
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationKind
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.animeDetails
import su.afk.yummy.tv.feature.details.details.DetailsState.Event
import su.afk.yummy.tv.feature.details.details.handler.DetailsLibraryHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsLibraryMutationResult
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsScreenDataHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsSubscriptionHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsVideoHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsExtrasHandler
import su.afk.yummy.tv.feature.details.details.handler.DetailsExtrasResult
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.feature.details.details.handler.DetailsVideosResult
import su.afk.yummy.tv.feature.details.details.handler.DetailsWatchTarget
import su.afk.yummy.tv.feature.details.details.model.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator

class DetailsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val permissionRequested = MutableStateFlow(false)
    private val appLifecycleSettingsStore: AppLifecycleSettingsStore = mockk(relaxed = true) {
        every { notificationPermissionRequested } returns permissionRequested
    }
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val commentsNavigator: ICommentsNavigator = mockk()
    private val reviewsNavigator: IReviewsNavigator = mockk()
    private val bloggerVideosNavigator: IBloggerVideosNavigator = mockk()
    private val imageViewNavigator: IImageViewNavigator = mockk()
    private val stringProvider: StringProvider = mockk()
    private val screenDataHandler: DetailsScreenDataHandler = mockk()
    private val libraryHandler: DetailsLibraryHandler = mockk()
    private val videoHandler: DetailsVideoHandler = mockk()
    private val subscriptionHandler: DetailsSubscriptionHandler = mockk()
    private val playerNavigationHandler: DetailsPlayerNavigationHandler = mockk()
    private val extrasHandler: DetailsExtrasHandler = mockk()
    private val userRating = MutableStateFlow<Int?>(null)
    private val tracker: AnalyticsTracker = mockk(relaxed = true)

    private val session = MutableStateFlow(AccountSession(isAuthorized = false, userId = 0))
    private val libraryState = MutableStateFlow(AnimeLibraryState(isInLibrary = false, isFavorite = false))
    private val navKey: NavKey = mockk()
    private val video = AnimeVideo(
        id = 1,
        episode = "1",
        dubbing = "Anidub",
        player = "Kodik",
        playerId = 1,
        iframeUrl = "https://player/1",
        durationSeconds = 1_400,
    )

    @Before
    fun setUp() {
        coEvery { playerNavigationHandler.confirmOpen(any()) } returns true
        every { stringProvider.get(any<Int>()) } returns "load error"
        every { screenDataHandler.preferredPlayer } returns flowOf(PreferredPlayer.NONE)
        every { screenDataHandler.yaniUserId } returns flowOf(0)
        every { screenDataHandler.anime4kDubbing(any()) } returns flowOf(null)
        every { screenDataHandler.askDubbingOnWatch } returns flowOf(false)
        every { screenDataHandler.detailsButtonOrder } returns flowOf(DetailsButtonAction.entries)
        every { screenDataHandler.observeLibraryState(any()) } returns libraryState
        every { screenDataHandler.observeWatchProgress(any()) } returns flowOf(emptyList())
        every { screenDataHandler.observeUserRating(any()) } returns userRating
        coEvery { extrasHandler.load(any()) } returns DetailsExtrasResult(emptyList(), null)
        coEvery { extrasHandler.loadRatingOverview(any()) } returns null
        every { screenDataHandler.observeAccountSession() } returns session
        coEvery { screenDataHandler.loadDetails(ANIME_ID) } returns Result.success(animeDetails(id = ANIME_ID, title = "Anime"))
        coEvery { screenDataHandler.refreshLibraryMetadata(any(), any(), any(), any()) } returns Unit
        coEvery { videoHandler.loadCached(any(), any()) } returns null
        coEvery { videoHandler.load(any(), any()) } returns Result.success(videosResult())
        coEvery { videoHandler.refresh(any(), any()) } returns Result.success(videosResult())
        coEvery { libraryHandler.refreshAuthorizedState(any()) } returns Result.success(null)
        every { subscriptionHandler.pendingSubscriptionStates(any()) } returns emptyMap()
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { detailsNavigator.getFullDetailsDest(any()) } returns navKey
        every { detailsNavigator.getRelationDest(any(), any(), any()) } returns navKey
        every { detailsNavigator.getEpisodesDest(any(), any()) } returns navKey
        every { detailsNavigator.getTrailersDest(any()) } returns navKey
        every { detailsNavigator.getSimilarDest(any()) } returns navKey
        every { detailsNavigator.getRatingDest(any()) } returns navKey
        every { detailsNavigator.getSubscriptionsDest(any()) } returns navKey
        every { commentsNavigator.getAnimeCommentsDest(any()) } returns navKey
        every { reviewsNavigator.list(any()) } returns navKey
        every { bloggerVideosNavigator.anime(any()) } returns navKey
    }

    private fun createViewModel() = DetailsViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        commentsNavigator = commentsNavigator,
        reviewsNavigator = reviewsNavigator,
        bloggerVideosNavigator = bloggerVideosNavigator,
        imageViewNavigator = imageViewNavigator,
        stringProvider = stringProvider,
        screenDataHandler = screenDataHandler,
        libraryHandler = libraryHandler,
        videoHandler = videoHandler,
        subscriptionHandler = subscriptionHandler,
        playerNavigationHandler = playerNavigationHandler,
        extrasHandler = extrasHandler,
        analytics = DetailsAnalytics(tracker),
        appLifecycleSettingsStore = appLifecycleSettingsStore,
    )

    private fun videosResult(
        vararg subscriptions: SubscriptionOption,
    ) = DetailsVideosResult(
        videos = listOf(video),
        videosState = VideosUiState.Content(persistentListOf(video)),
        subscriptions = subscriptions.toList(),
    )

    private fun option(key: String, subscribed: Boolean = false) = SubscriptionOption(
        key = key,
        playerId = 1,
        player = "Kodik",
        dubbing = key,
        episodesCount = 1,
        subscriptionVideoId = 10,
        isSubscribed = subscribed,
    )

    @Test
    fun `loads the details and the videos`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("Anime", state.details?.title)
        assertTrue(state.videosState is VideosUiState.Content)
        assertEquals(DetailsButtonAction.entries, state.detailsButtonOrder)
    }

    @Test
    fun `trailers and rating overview land in the state`() {
        val trailer = AnimeTrailer(iframeUrl = "https://www.youtube.com/embed/abc")
        val overview = DetailsRatingOverview(average = 8.0, votes = 3)
        coEvery { extrasHandler.load(ANIME_ID) } returns DetailsExtrasResult(listOf(trailer), overview)

        val state = createViewModel().currentState

        assertEquals(listOf(trailer), state.trailers)
        assertEquals(overview, state.ratingOverview)
        assertFalse(state.isExtrasLoading)
    }

    @Test
    fun `changed user rating refreshes the rating overview`() {
        val refreshed = DetailsRatingOverview(average = 9.0, votes = 4)
        coEvery { extrasHandler.loadRatingOverview(ANIME_ID) } returns refreshed
        val vm = createViewModel()

        userRating.value = 9

        assertEquals(refreshed, vm.currentState.ratingOverview)
        coVerify(exactly = 1) { extrasHandler.loadRatingOverview(ANIME_ID) }
    }

    @Test
    fun `screenshot from the details row opens the gallery at its index`() {
        val screenshots = listOf(
            AnimeScreenshot(id = 1, episode = null, small = "s1", full = "f1"),
            AnimeScreenshot(id = 2, episode = null, small = "s2", full = "f2"),
        )
        coEvery { screenDataHandler.loadDetails(ANIME_ID) } returns
            Result.success(animeDetails(id = ANIME_ID, screenshots = screenshots))
        every { imageViewNavigator(imageUrl = any(), imageUrls = any(), selectedIndex = any()) } returns navKey
        val vm = createViewModel()

        vm.setEvent(Event.ScreenshotSelected(1))

        verify { imageViewNavigator(imageUrl = "f2", imageUrls = listOf("f1", "f2"), selectedIndex = 1) }
        verify { nav.navigate(navKey) }
    }

    @Test
    fun `share sends the site link of the title`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ShareSelected)

        assertEquals(
            listOf(DetailsState.Effect.ShareAnime(title = "Anime", url = "https://ru.yummyani.me/catalog/item/url")),
            effects,
        )
    }

    @Test
    fun `cached videos are used without a network request for a guest`() {
        coEvery { videoHandler.loadCached(any(), any()) } returns videosResult()

        createViewModel()

        coVerify(exactly = 0) { videoHandler.load(any(), any()) }
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { screenDataHandler.loadDetails(ANIME_ID) } returns Result.failure(IllegalStateException("boom"))
        val vm = createViewModel()
        assertEquals("boom", (vm.currentState.error as UiText.Failure).cause.message)
        coEvery { screenDataHandler.loadDetails(ANIME_ID) } returns Result.success(animeDetails(id = ANIME_ID))

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertNotNull(vm.currentState.details)
    }

    @Test
    fun `failed videos are shown as an error state`() {
        coEvery { videoHandler.load(any(), any()) } returns Result.failure(IllegalStateException("offline"))

        val error = createViewModel().currentState.videosState as VideosUiState.Error
        assertEquals("offline", (error.message as UiText.Failure).cause.message)
    }

    @Test
    fun `library state of the store is mirrored`() {
        val vm = createViewModel()

        libraryState.value = AnimeLibraryState(isInLibrary = true, isFavorite = true)

        assertTrue(vm.currentState.isInLibrary)
        assertTrue(vm.currentState.isFavorite)
    }

    @Test
    fun `adding to the library asks for a list first`() {
        val vm = createViewModel()

        vm.setEvent(Event.LibraryToggled)
        assertTrue(vm.currentState.showLibraryListPicker)

        coEvery { libraryHandler.addToLibrary(any(), any(), any(), any(), any(), any(), any()) } returns
            DetailsLibraryMutationResult.Success
        vm.setEvent(Event.LibraryListSelected(UserAnimeList.PLANNED))

        assertFalse(vm.currentState.showLibraryListPicker)
        assertTrue(vm.currentState.isInLibrary)
        assertEquals(UserAnimeList.PLANNED, vm.currentState.libraryList)
    }

    @Test
    fun `failed add is rolled back`() {
        coEvery { libraryHandler.addToLibrary(any(), any(), any(), any(), any(), any(), any()) } returns
            DetailsLibraryMutationResult.RollbackLibrary(isInLibrary = false, libraryList = null)
        val vm = createViewModel()

        vm.setEvent(Event.LibraryListSelected(UserAnimeList.WATCHING))

        assertFalse(vm.currentState.isInLibrary)
        assertNull(vm.currentState.libraryList)
    }

    @Test
    fun `title in the library is removed by the toggle`() {
        coEvery { libraryHandler.removeFromLibrary(any(), any(), any(), any(), any(), any()) } returns
            DetailsLibraryMutationResult.Success
        libraryState.value = AnimeLibraryState(isInLibrary = true, isFavorite = false)
        val vm = createViewModel()

        vm.setEvent(Event.LibraryToggled)

        assertFalse(vm.currentState.isInLibrary)
        coVerify(exactly = 1) { libraryHandler.removeFromLibrary(ANIME_ID, any(), any(), true, false, false) }
    }

    @Test
    fun `favorite toggle is applied and can be rolled back`() {
        coEvery { libraryHandler.setFavorite(any(), any(), true, any(), any()) } returns DetailsLibraryMutationResult.Success
        val vm = createViewModel()

        vm.setEvent(Event.FavoriteToggled)
        assertTrue(vm.currentState.isFavorite)

        coEvery { libraryHandler.setFavorite(any(), any(), false, any(), any()) } returns
            DetailsLibraryMutationResult.RollbackFavorite(isFavorite = true)
        vm.setEvent(Event.FavoriteToggled)
        assertTrue(vm.currentState.isFavorite)
    }

    @Test
    fun `watch continues from the saved target`() {
        val source: su.afk.yummy.tv.feature.player.PlayerVideoSource = mockk(relaxed = true)
        every { videoHandler.resolveWatchTarget(any(), any(), any()) } returns DetailsWatchTarget.Continue(source)
        every {
            playerNavigationHandler.getPlayerDestination(any<su.afk.yummy.tv.feature.player.PlayerVideoSource>(), any(), any(), any(), any(), any())
        } returns navKey
        val vm = createViewModel()

        vm.setEvent(Event.WatchSelected)

        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `watch without a saved target picks the player for the first video`() {
        every { videoHandler.resolveWatchTarget(any(), any(), any()) } returns DetailsWatchTarget.Initial(video)
        every { playerNavigationHandler.selectPlayer(any(), any(), any(), any()) } returns DetailsPlayerSelection.Navigate(video)
        every {
            playerNavigationHandler.getPlayerDestination(any<AnimeVideo>(), any(), any(), any(), any(), any())
        } returns navKey
        val vm = createViewModel()

        vm.setEvent(Event.WatchSelected)

        verify(exactly = 1) { nav.navigate(navKey) }
        assertFalse(vm.currentState.isWatchLaunchPending)
    }

    @Test
    fun `watch with nothing to play only clears the pending flag`() {
        every { videoHandler.resolveWatchTarget(any(), any(), any()) } returns null
        val vm = createViewModel()

        vm.setEvent(Event.WatchSelected)

        verify(exactly = 0) { nav.navigate(any()) }
        assertFalse(vm.currentState.isWatchLaunchPending)
    }

    @Test
    fun `signing in refreshes the library and the videos`() {
        val vm = createViewModel()

        session.value = AccountSession(isAuthorized = true, userId = 1)

        assertTrue(vm.currentState.isSignedIn)
        coVerify(atLeast = 1) { libraryHandler.refreshAuthorizedState(ANIME_ID) }
        coVerify(atLeast = 1) { videoHandler.refresh(ANIME_ID, any()) }
    }

    @Test
    fun `signed in user toggles a subscription`() {
        coEvery { videoHandler.load(any(), any()) } returns Result.success(videosResult(option("a")))
        coEvery { videoHandler.refresh(any(), any()) } returns Result.success(videosResult(option("a")))
        coEvery { subscriptionHandler.commitSubscriptionChange(ANIME_ID, any(), true) } returns true
        coEvery { subscriptionHandler.reloadSubscriptions(ANIME_ID) } returns Result.success(listOf(option("a", subscribed = true)))
        session.value = AccountSession(isAuthorized = true, userId = 1)
        val vm = createViewModel()

        vm.setEvent(Event.SubscriptionToggled("a"))

        assertTrue(vm.currentState.subscriptions.single().isSubscribed)
    }

    @Test
    fun `guest cannot toggle a subscription`() {
        val vm = createViewModel()

        vm.setEvent(Event.SubscriptionToggled("a"))

        coVerify(exactly = 0) { subscriptionHandler.commitSubscriptionChange(any(), any(), any()) }
    }

    @Test
    fun `guest subscriptions teaser opens and sign in switches to the account tab`() {
        val vm = createViewModel()

        vm.setEvent(Event.SubscriptionsSignInPromptSelected)
        assertTrue(vm.currentState.showSubscriptionsSignInPrompt)

        vm.setEvent(Event.SubscriptionsSignInPromptDismissed)
        assertFalse(vm.currentState.showSubscriptionsSignInPrompt)

        vm.setEvent(Event.SubscriptionsSignInPromptSelected)
        vm.setEvent(Event.SignInSelected)

        assertFalse(vm.currentState.showSubscriptionsSignInPrompt)
        verify(exactly = 1) { nav.switchRoot(RootTab.ACCOUNT, any()) }
    }

    @Test
    fun `balancer picker can be dismissed`() {
        val vm = createViewModel()

        vm.setEvent(Event.BalancerPickerDismissed)
        vm.setEvent(Event.DubbingPickerDismissed)

        assertNull(vm.currentState.pendingBalancerSelection)
        assertNull(vm.currentState.pendingDubbingSelection)
    }

    @Test
    fun `info sheet opens and closes`() {
        val vm = createViewModel()

        vm.setEvent(Event.InfoSelected)
        assertTrue(vm.currentState.showInfoSheet)
        vm.setEvent(Event.InfoDismissed)
        assertFalse(vm.currentState.showInfoSheet)
    }

    @Test
    fun `genre click opens genre relation screen`() {
        val vm = createViewModel()

        vm.setEvent(Event.GenreSelected(5))

        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.GENRE, 5, null) }
        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `studio and director clicks open relation screens`() {
        val vm = createViewModel()

        vm.setEvent(Event.StudioSelected(7, "url"))
        vm.setEvent(Event.DirectorSelected(8))

        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.STUDIO, 7, "url") }
        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.DIRECTOR, 8, null) }
    }

    @Test
    fun `navigation events open their screens`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(9))
        vm.setEvent(Event.FullDetailsSelected)
        vm.setEvent(Event.EpisodesSelected)
        vm.setEvent(Event.TrailersSelected)
        vm.setEvent(Event.SimilarSelected)
        vm.setEvent(Event.RatingScreenSelected)
        vm.setEvent(Event.CommentsSelected)
        vm.setEvent(Event.ReviewsSelected)
        vm.setEvent(Event.BloggerVideosSelected)
        vm.setEvent(Event.SubscriptionsRouteSelected)
        vm.setEvent(Event.BackSelected)

        verify(exactly = 10) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(9) }
    }

    @Test
    fun `permission request flag is mirrored and remembered`() {
        val vm = createViewModel()
        assertFalse(vm.currentState.notificationPermissionRequested)

        vm.setEvent(Event.NotificationPermissionRequested)
        permissionRequested.value = true

        assertTrue(vm.currentState.notificationPermissionRequested)
        coVerify(exactly = 1) { appLifecycleSettingsStore.markNotificationPermissionRequested() }
    }

    private companion object {
        const val ANIME_ID = 4
    }
}

package su.afk.yummy.tv.feature.home

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlags
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.model.settings.SupportPromptSnapshot
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.anime.usecase.SetAnimeRecommendationIgnoredUseCase
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.HomeState.Effect
import su.afk.yummy.tv.feature.home.HomeState.Event
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import su.afk.yummy.tv.feature.search.ISearchNavigator

class HomeViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val collectionNavigator: ICollectionNavigator = mockk()
    private val reviewsNavigator: IReviewsNavigator = mockk()
    private val bloggerVideosNavigator: IBloggerVideosNavigator = mockk()
    private val scheduleNavigator: IScheduleNavigator = mockk()
    private val searchNavigator: ISearchNavigator = mockk()
    private val getHomeFeed: GetHomeFeedUseCase = mockk()
    private val getBloggerVideos: GetBloggerVideosUseCase = mockk()
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase = mockk()
    private val refreshHomeFeed: RefreshHomeFeedUseCase = mockk()
    private val setAnimeRecommendationIgnored: SetAnimeRecommendationIgnoredUseCase = mockk()
    private val observeContinueWatching: ObserveContinueWatchingUseCase = mockk()
    private val stringProvider: StringProvider = mockk()
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase = mockk()
    private val playerNavigator: IPlayerNavigator = mockk()
    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val featureToggleProvider: FeatureToggleProvider = mockk()
    private val featureToggleUpdateObserver: FeatureToggleUpdateObserver = mockk()
    private val analytics: HomeAnalytics = mockk(relaxed = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)

    private val continueWatching = MutableStateFlow<List<HomeContinueWatchingItem>>(emptyList())
    private val hiddenIds = MutableStateFlow<Set<Int>>(emptySet())
    private val userId = MutableStateFlow(USER_ID)
    private val supportPrompt = MutableStateFlow(SupportPromptSnapshot(dismissed = true, firstEligibleTimeMs = 0L))
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { observeContinueWatching() } returns continueWatching
        every { settingsStore.hiddenRecommendationIds } returns hiddenIds
        every { settingsStore.yaniUserId } returns userId
        every { settingsStore.yaniContentLanguage } returns emptyFlow()
        every { settingsStore.supportPromptSnapshot } returns supportPrompt
        every { settingsStore.lastSeenAnnouncementId } returns flowOf("")
        every { featureToggleUpdateObserver.currentActivationId } returns 1L
        every { featureToggleUpdateObserver.updates } returns flowOf()
        every { featureToggleProvider.getString(any()) } returns ""
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { collectionNavigator.getCollectionDest(any()) } returns navKey
        every { collectionNavigator.getCollectionsCatalogDest() } returns navKey
        every { reviewsNavigator.feed() } returns navKey
        every { bloggerVideosNavigator.feed() } returns navKey
        every { bloggerVideosNavigator.video(any()) } returns navKey
        every { scheduleNavigator.getScheduleDest() } returns navKey
        every { searchNavigator.getSearchDest(any()) } returns navKey
        coEvery { getCachedHomeFeed() } returns null
        coEvery { getHomeFeed() } returns feed()
        coEvery { refreshHomeFeed() } returns feed()
        coEvery { getBloggerVideos(any(), any(), any(), any(), any()) } returns emptyList()
        coEvery { setAnimeRecommendationIgnored(any(), any()) } returns true
    }

    private fun createViewModel() = HomeViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        collectionNavigator = collectionNavigator,
        reviewsNavigator = reviewsNavigator,
        bloggerVideosNavigator = bloggerVideosNavigator,
        scheduleNavigator = scheduleNavigator,
        searchNavigator = searchNavigator,
        getHomeFeed = getHomeFeed,
        getBloggerVideos = getBloggerVideos,
        getCachedHomeFeed = getCachedHomeFeed,
        refreshHomeFeed = refreshHomeFeed,
        setAnimeRecommendationIgnored = setAnimeRecommendationIgnored,
        observeContinueWatching = observeContinueWatching,
        stringProvider = stringProvider,
        resolveContinueWatchingLaunch = resolveContinueWatchingLaunch,
        playerNavigator = playerNavigator,
        settingsStore = settingsStore,
        featureToggleProvider = featureToggleProvider,
        featureToggleUpdateObserver = featureToggleUpdateObserver,
        analytics = analytics,
        analyticsTracker = analyticsTracker,
    )

    private fun item(id: Int) = HomeFeedItem(id, "Title $id", "", null, null, null, mockk())

    private fun feed(recommendations: List<Int> = listOf(1, 2)) = HomeFeed(
        heroItems = emptyList(),
        sections = listOf(
            HomeFeedSection(HomeFeedSectionType.SCHEDULE, "Schedule", listOf(item(100))),
            HomeFeedSection(HomeFeedSectionType.RECOMMENDATIONS, "For you", recommendations.map(::item)),
        ),
    )

    private fun watching(videoId: Int = 0, episode: String = "", url: String = "") = HomeContinueWatchingItem(
        animeId = 5,
        animeTitle = "Anime",
        description = "",
        poster = null,
        videoId = videoId,
        episode = episode,
        episodeUrl = url,
        positionMs = 0L,
        durationMs = 0L,
        updatedAt = 0L,
        playerName = "",
        dubbing = "",
        screenshotUrl = "",
    )

    private fun HomeFeed.recommendationIds() =
        sections.single { it.type == HomeFeedSectionType.RECOMMENDATIONS }.items.map { it.id }

    @Test
    fun `loads the feed without the schedule section and remembers that it existed`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.hasSchedule)
        assertEquals(listOf(HomeFeedSectionType.RECOMMENDATIONS), state.feed?.sections?.map { it.type })
        verify(exactly = 1) { analytics.eventScreenOpened() }
    }

    @Test
    fun `cached feed is shown first and then replaced by the fresh one`() {
        coEvery { getCachedHomeFeed() } returns feed(recommendations = listOf(9))
        coEvery { getHomeFeed() } returns feed(recommendations = listOf(1, 2))

        val state = createViewModel().currentState

        assertEquals(listOf(1, 2), state.feed?.recommendationIds())
    }

    @Test
    fun `failed load without a feed shows the error and retry recovers`() {
        coEvery { getHomeFeed() } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        assertFalse(vm.currentState.isLoading)
        coEvery { getHomeFeed() } returns feed()

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertNotNull(vm.currentState.feed)
    }

    @Test
    fun `failed refresh keeps the shown feed`() {
        val vm = createViewModel()
        coEvery { refreshHomeFeed() } throws IllegalStateException("boom")

        vm.setEvent(Event.RefreshRequested)

        assertNotNull(vm.currentState.feed)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `refresh replaces the feed`() {
        val vm = createViewModel()
        coEvery { refreshHomeFeed() } returns feed(recommendations = listOf(7))

        vm.setEvent(Event.RefreshRequested)

        assertEquals(listOf(7), vm.currentState.feed?.recommendationIds())
    }

    @Test
    fun `continue watching list is mirrored`() {
        val vm = createViewModel()

        continueWatching.value = listOf(watching(videoId = 1))

        assertTrue(vm.currentState.isContinueWatchingLoaded)
        assertEquals(1, vm.currentState.continueWatching.size)
    }

    @Test
    fun `resume syncs only the continue watching part of the cached feed`() {
        val vm = createViewModel()
        val newItem = watching(videoId = 3)
        coEvery { getCachedHomeFeed() } returns feed().copy(continueWatchingItems = listOf(newItem))

        vm.setEvent(Event.ScreenResumed)

        assertEquals(listOf(newItem), vm.currentState.feed?.continueWatchingItems)
    }

    @Test
    fun `blogger videos load and failure shows an error`() {
        val video: BloggerVideo = mockk()
        coEvery { getBloggerVideos(any(), any(), any(), any(), any()) } returns listOf(video)
        val vm = createViewModel()
        assertEquals(listOf(video), vm.currentState.bloggerVideos)
        assertFalse(vm.currentState.isBloggerVideosLoading)

        coEvery { getBloggerVideos(any(), any(), any(), any(), any()) } throws IllegalStateException("boom")
        vm.setEvent(Event.BloggerVideosRetrySelected)

        assertEquals("boom", vm.currentState.bloggerVideosError)
    }

    @Test
    fun `hiding a recommendation removes it and offers an undo`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RecommendationHideRequested(1))

        assertEquals(listOf(2), vm.currentState.feed?.recommendationIds())
        assertTrue(vm.currentState.pendingRecommendationIds.isEmpty())
        assertEquals(1, effects.size)
        assertTrue(effects.single() is Effect.ShowRecommendationUndo)
        verify(exactly = 1) { analytics.eventRecommendationHidden(1) }
    }

    @Test
    fun `rejected hide rolls the recommendation back and shows a toast`() = runTest {
        coEvery { setAnimeRecommendationIgnored(1, true) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RecommendationHideRequested(1))

        assertEquals(listOf(1, 2), vm.currentState.feed?.recommendationIds())
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `guest cannot hide a recommendation`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RecommendationHideRequested(1))

        assertEquals(listOf(1, 2), vm.currentState.feed?.recommendationIds())
        assertTrue(effects.single() is Effect.ShowToast)
        coVerify(exactly = 0) { setAnimeRecommendationIgnored(any(), any()) }
    }

    @Test
    fun `restore returns the recommendation`() {
        hiddenIds.value = setOf(1)
        val vm = createViewModel()
        assertEquals(listOf(2), vm.currentState.feed?.recommendationIds())

        vm.setEvent(Event.RecommendationRestoreRequested(1))

        assertEquals(listOf(1, 2), vm.currentState.feed?.recommendationIds())
        verify(exactly = 1) { analytics.eventRecommendationRestored(1) }
    }

    @Test
    fun `support prompt is shown once the waiting period has passed`() {
        supportPrompt.value = SupportPromptSnapshot(dismissed = false, firstEligibleTimeMs = 0L)

        val vm = createViewModel()

        assertTrue(vm.currentState.supportPromptVisible)
        vm.setEvent(Event.SupportPromptDismissed)
        assertFalse(vm.currentState.supportPromptVisible)
        coVerify(atLeast = 1) { settingsStore.dismissSupportPrompt() }
    }

    @Test
    fun `dismissed support prompt stays hidden`() {
        val vm = createViewModel()

        assertFalse(vm.currentState.supportPromptVisible)
    }

    @Test
    fun `enabled announcement is shown and remembered once dismissed`() {
        every { featureToggleProvider.getString(FeatureFlags.announcementId) } returns "7"
        every { featureToggleProvider.getString(FeatureFlags.announcementMessage) } returns "Hello"
        every { featureToggleProvider.getString(FeatureFlags.announcementTitle) } returns " "
        val vm = createViewModel()

        val announcement = vm.currentState.announcement
        assertEquals("7", announcement?.id)
        assertEquals("Hello", announcement?.message)
        assertNull(announcement?.title)

        vm.setEvent(Event.AnnouncementDismissed)

        assertNull(vm.currentState.announcement)
        coVerify(exactly = 1) { settingsStore.markAnnouncementSeen("7") }
    }

    @Test
    fun `disabled or already seen announcement is not shown`() {
        every { featureToggleProvider.getString(FeatureFlags.announcementId) } returns "0"
        every { featureToggleProvider.getString(FeatureFlags.announcementMessage) } returns "Hello"
        assertNull(createViewModel().currentState.announcement)

        every { featureToggleProvider.getString(FeatureFlags.announcementId) } returns "7"
        every { settingsStore.lastSeenAnnouncementId } returns flowOf("7")
        assertNull(createViewModel().currentState.announcement)
    }

    @Test
    fun `continue watching without a playable target opens the details`() {
        val vm = createViewModel()

        vm.setEvent(Event.ContinueWatchingSelected(watching()))

        verify(exactly = 1) { detailsNavigator.getDetailsDest(5) }
        verify(exactly = 1) { nav.navigate(navKey) }
        coVerify(exactly = 0) { resolveContinueWatchingLaunch(any(), any()) }
    }

    @Test
    fun `navigation events open their destinations`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(3))
        vm.setEvent(Event.CollectionSelected(4))
        vm.setEvent(Event.CollectionsCatalogSelected)
        vm.setEvent(Event.ScheduleSelected)
        vm.setEvent(Event.SearchSelected)
        vm.setEvent(Event.ReviewsSelected)
        vm.setEvent(Event.BloggerVideosSelected)
        vm.setEvent(Event.BloggerVideoSelected(mockk<BloggerVideo> { every { id } returns 8 }))

        verify(exactly = 8) { nav.navigate(navKey) }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(3) }
        verify(exactly = 1) { collectionNavigator.getCollectionDest(4) }
        verify(exactly = 1) { bloggerVideosNavigator.video(8) }
    }

    private companion object {
        const val USER_ID = 5
    }
}

package su.afk.yummy.tv.feature.community

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import su.afk.yummy.tv.domain.bloggers.usecase.CachedBloggerVideosUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.posts.model.PostSummary
import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import su.afk.yummy.tv.domain.posts.usecase.CachedPostsUseCase
import su.afk.yummy.tv.domain.posts.usecase.GetPostsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.community.model.CommunitySection
import su.afk.yummy.tv.feature.glossary.IGlossaryNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator

class CommunityViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val postsNavigator: IPostsNavigator = mockk()
    private val bloggerVideosNavigator: IBloggerVideosNavigator = mockk()
    private val messagesNavigator: IMessagesNavigator = mockk()
    private val sitePagesNavigator: ISitePagesNavigator = mockk()
    private val glossaryNavigator: IGlossaryNavigator = mockk()
    private val accountNavigator: IAccountNavigator = mockk()
    private val reviewsNavigator: IReviewsNavigator = mockk()
    private val collectionNavigator: ICollectionNavigator = mockk()
    private val scheduleNavigator: IScheduleNavigator = mockk()
    private val postsRepository: PostsRepository = mockk()
    private val videosRepository: BloggerVideosRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val session = MutableStateFlow(AccountSession(isAuthorized = true, userId = USER_ID))
    private val navKey: NavKey = mockk()

    private val post: PostSummary = mockk()
    private val video: BloggerVideo = mockk()

    @Before
    fun setUp() {
        every { postsNavigator.list() } returns navKey
        every { postsNavigator.details(any()) } returns navKey
        every { bloggerVideosNavigator.feed() } returns navKey
        every { bloggerVideosNavigator.video(any()) } returns navKey
        every { messagesNavigator.dialogs() } returns navKey
        every { sitePagesNavigator.pages() } returns navKey
        every { glossaryNavigator.getGlossaryDest() } returns navKey
        every { accountNavigator.getUserSearchDest() } returns navKey
        every { reviewsNavigator.feed() } returns navKey
        every { collectionNavigator.getCollectionsCatalogDest() } returns navKey
        every { accountNavigator.getUserFriendsDest(any()) } returns navKey
        every { scheduleNavigator.getScheduleDest() } returns navKey
        every { accountRepository.observeSession() } returns session
        coEvery { postsRepository.posts(any(), any(), any(), any(), any()) } returns listOf(post)
        coEvery { videosRepository.getVideos(any(), any(), any(), any(), any(), any()) } returns listOf(video)
    }

    private fun createViewModel() = CommunityViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        postsNavigator = postsNavigator,
        bloggerVideosNavigator = bloggerVideosNavigator,
        messagesNavigator = messagesNavigator,
        sitePagesNavigator = sitePagesNavigator,
        glossaryNavigator = glossaryNavigator,
        accountNavigator = accountNavigator,
        reviewsNavigator = reviewsNavigator,
        collectionNavigator = collectionNavigator,
        scheduleNavigator = scheduleNavigator,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        getPosts = GetPostsUseCase(postsRepository),
        cachedPosts = CachedPostsUseCase(postsRepository),
        cachedBloggerVideos = CachedBloggerVideosUseCase(videosRepository),
        getBloggerVideos = GetBloggerVideosUseCase(videosRepository),
    )

    @Test
    fun `news and videos are loaded on start`() {
        val state = createViewModel().currentState

        assertEquals(listOf(post), state.news)
        assertEquals(listOf(video), state.videos)
        assertFalse(state.isNewsLoading)
        assertFalse(state.isVideosLoading)
        assertNull(state.newsError)
        assertNull(state.videosError)
    }

    @Test
    fun `failed news load shows an error and retry recovers`() {
        coEvery { postsRepository.posts(any(), any(), any(), any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()

        assertTrue(vm.currentState.news.isEmpty())
        assertNotNull(vm.currentState.newsError)
        assertEquals(listOf(video), vm.currentState.videos)

        coEvery { postsRepository.posts(any(), any(), any(), any(), any()) } returns listOf(post)
        vm.setEvent(CommunityState.Event.NewsRetrySelected)

        assertEquals(listOf(post), vm.currentState.news)
        assertNull(vm.currentState.newsError)
    }

    @Test
    fun `failed videos load shows an error and retry recovers`() {
        coEvery { videosRepository.getVideos(any(), any(), any(), any(), any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()

        assertTrue(vm.currentState.videos.isEmpty())
        assertNotNull(vm.currentState.videosError)

        coEvery { videosRepository.getVideos(any(), any(), any(), any(), any(), any()) } returns listOf(video)
        vm.setEvent(CommunityState.Event.VideosRetrySelected)

        assertEquals(listOf(video), vm.currentState.videos)
        assertNull(vm.currentState.videosError)
    }

    @Test
    fun `failed refresh keeps the feeds that are already shown`() {
        val vm = createViewModel()
        coEvery { postsRepository.posts(any(), any(), any(), any(), any()) } throws IllegalStateException("boom")
        coEvery { videosRepository.getVideos(any(), any(), any(), any(), any(), any()) } throws IllegalStateException("boom")

        vm.setEvent(CommunityState.Event.RefreshSelected)

        val state = vm.currentState
        assertEquals(listOf(post), state.news)
        assertEquals(listOf(video), state.videos)
        assertNull(state.newsError)
        assertNull(state.videosError)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `every section tile opens its destination`() {
        val vm = createViewModel()

        CommunitySection.entries.forEach { vm.setEvent(CommunityState.Event.SectionSelected(it)) }

        verify(exactly = CommunitySection.entries.size) { nav.navigate(navKey) }
        verify(exactly = 1) { reviewsNavigator.feed() }
        verify(exactly = 1) { collectionNavigator.getCollectionsCatalogDest() }
        verify(exactly = 1) { scheduleNavigator.getScheduleDest() }
        verify(exactly = 1) { messagesNavigator.dialogs() }
        verify(exactly = 1) { accountNavigator.getUserSearchDest() }
        verify(exactly = 1) { accountNavigator.getUserFriendsDest(USER_ID) }
        verify(exactly = 1) { sitePagesNavigator.pages() }
        verify(exactly = 1) { glossaryNavigator.getGlossaryDest() }
    }

    @Test
    fun `friends tile sends a guest to the profile tab instead of the friends list`() {
        session.value = AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()

        vm.setEvent(CommunityState.Event.SectionSelected(CommunitySection.FRIENDS))

        verify(exactly = 1) { nav.switchRoot(RootTab.ACCOUNT) }
        verify(exactly = 0) { accountNavigator.getUserFriendsDest(any()) }
        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `all-news and all-videos buttons open the full lists`() {
        val vm = createViewModel()

        vm.setEvent(CommunityState.Event.AllNewsSelected)
        vm.setEvent(CommunityState.Event.AllVideosSelected)

        verify(exactly = 1) { postsNavigator.list() }
        verify(exactly = 1) { bloggerVideosNavigator.feed() }
        verify(exactly = 2) { nav.navigate(navKey) }
    }

    @Test
    fun `selecting a news card or a video opens its details`() {
        val vm = createViewModel()

        vm.setEvent(CommunityState.Event.NewsSelected(POST_ID))
        vm.setEvent(CommunityState.Event.VideoSelected(VIDEO_ID))

        verify(exactly = 1) { postsNavigator.details(POST_ID) }
        verify(exactly = 1) { bloggerVideosNavigator.video(VIDEO_ID) }
        verify(exactly = 2) { nav.navigate(navKey) }
    }

    private companion object {
        const val USER_ID = 3
        const val POST_ID = 5
        const val VIDEO_ID = 8
    }
}

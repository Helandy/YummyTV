package su.afk.yummy.tv.feature.bloggers.details

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerDetailsUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.SetBloggerSubscribedUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsState.Effect
import su.afk.yummy.tv.feature.bloggers.details.BloggerDetailsState.Event
import su.afk.yummy.tv.feature.bloggers.presentation.R

class BloggerDetailsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val navigator: IBloggerVideosNavigator = mockk()
    private val repository: BloggerVideosRepository = mockk()
    private val strings: StringProvider = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val userId = MutableStateFlow(USER_ID)
    private val videoKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { strings.get(R.string.bloggers_auth_required) } returns "auth required"
        every { settingsStore.yaniUserId } returns userId
        every { navigator.video(any()) } returns videoKey
        coEvery { repository.getBlogger(BLOGGER_ID) } returns blogger()
        coEvery { repository.getVideos(any(), any(), any(), any(), any()) } returns listOf(video(1), video(2))
    }

    private fun createViewModel() = BloggerDetailsViewModel(
        bloggerId = BLOGGER_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        navigator = navigator,
        getDetails = GetBloggerDetailsUseCase(repository),
        getVideos = GetBloggerVideosUseCase(repository),
        setSubscribed = SetBloggerSubscribedUseCase(repository),
        strings = strings,
        settingsStore = settingsStore,
    )

    private fun blogger(subscribed: Boolean = false) = BloggerDetails(
        id = BLOGGER_ID,
        nickname = "blogger",
        avatarUrl = null,
        subscribers = 10,
        videosCount = 2,
        isSubscribed = subscribed,
        categories = listOf(BloggerVideoCategory("all", "All")),
    )

    private fun video(id: Int) = BloggerVideo(
        id = id,
        title = "video $id",
        description = "",
        previewUrl = null,
        iframeUrl = "",
        publishedAt = 0L,
        views = 0L,
        hasSpoiler = false,
        category = BloggerVideoCategory("all", "All"),
        creator = Blogger(BLOGGER_ID, "blogger", null),
    )

    @Test
    fun `loads the blogger with the deduplicated videos`() {
        coEvery { repository.getVideos(any(), any(), any(), any(), any()) } returns listOf(video(1), video(1), video(2))

        val state = createViewModel().currentState

        assertFalse(state.loading)
        assertEquals("blogger", state.blogger?.nickname)
        assertEquals(listOf(1, 2), state.videos.map { it.id })
        assertEquals(USER_ID, state.currentUserId)
    }

    @Test
    fun `failed load shows the parsed error and retry recovers`() {
        coEvery { repository.getBlogger(BLOGGER_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("parsed message", vm.currentState.error)
        assertFalse(vm.currentState.loading)
        verify(exactly = 0) { errorHandler.parse(any(), true, any(), any()) }
        coEvery { repository.getBlogger(BLOGGER_ID) } returns blogger()

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals("blogger", vm.currentState.blogger?.nickname)
    }

    @Test
    fun `subscribing updates the flag and the subscribers count`() {
        coEvery { repository.setSubscribed(BLOGGER_ID, true) } returns 11
        val vm = createViewModel()

        vm.setEvent(Event.SubscribeSelected)

        assertTrue(vm.currentState.blogger!!.isSubscribed)
        assertEquals(11, vm.currentState.blogger!!.subscribers)
        assertFalse(vm.currentState.subscribing)
    }

    @Test
    fun `subscribed blogger is unsubscribed on the next toggle`() {
        coEvery { repository.getBlogger(BLOGGER_ID) } returns blogger(subscribed = true)
        coEvery { repository.setSubscribed(BLOGGER_ID, false) } returns 9
        val vm = createViewModel()

        vm.setEvent(Event.SubscribeSelected)

        assertFalse(vm.currentState.blogger!!.isSubscribed)
        coVerify(exactly = 1) { repository.setSubscribed(BLOGGER_ID, false) }
    }

    @Test
    fun `failed subscription rolls back and shows a toast`() = runTest {
        coEvery { repository.setSubscribed(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.SubscribeSelected)

        assertFalse(vm.currentState.blogger!!.isSubscribed)
        assertFalse(vm.currentState.subscribing)
        assertEquals(listOf<Effect>(Effect.ShowToast("parsed message")), effects)
    }

    @Test
    fun `guest is asked to sign in instead of subscribing`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.SubscribeSelected)

        assertEquals(listOf<Effect>(Effect.ShowToast("auth required")), effects)
        coVerify(exactly = 0) { repository.setSubscribed(any(), any()) }
    }

    @Test
    fun `video selection opens the video`() {
        createViewModel().setEvent(Event.VideoSelected(2))

        verify(exactly = 1) { navigator.video(2) }
        verify(exactly = 1) { nav.navigate(videoKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val BLOGGER_ID = 3
        const val USER_ID = 5
    }
}

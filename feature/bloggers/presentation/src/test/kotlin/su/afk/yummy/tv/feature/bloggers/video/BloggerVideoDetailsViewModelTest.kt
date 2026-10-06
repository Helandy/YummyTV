package su.afk.yummy.tv.feature.bloggers.video

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
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideoDetailsUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.SetBloggerVideoVoteUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.presentation.R
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsState.Effect
import su.afk.yummy.tv.feature.bloggers.video.BloggerVideoDetailsState.Event
import su.afk.yummy.tv.feature.comments.ICommentsNavigator

class BloggerVideoDetailsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val navigator: IBloggerVideosNavigator = mockk()
    private val commentsNavigator: ICommentsNavigator = mockk()
    private val repository: BloggerVideosRepository = mockk()
    private val strings: StringProvider = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val userId = MutableStateFlow(USER_ID)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { strings.get(R.string.bloggers_auth_required) } returns "auth required"
        every { settingsStore.yaniUserId } returns userId
        every { navigator.blogger(any()) } returns navKey
        every { commentsNavigator.getCommentsDest(any(), any()) } returns navKey
        coEvery { repository.getVideo(VIDEO_ID) } returns video()
    }

    private fun createViewModel() = BloggerVideoDetailsViewModel(
        videoId = VIDEO_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        navigator = navigator,
        commentsNavigator = commentsNavigator,
        getDetails = GetBloggerVideoDetailsUseCase(repository),
        setVote = SetBloggerVideoVoteUseCase(repository),
        strings = strings,
        settingsStore = settingsStore,
    )

    private fun video(reaction: BloggerVideoReaction = BloggerVideoReaction(likes = 1, dislikes = 0)) = BloggerVideo(
        id = VIDEO_ID,
        title = "video",
        description = "",
        previewUrl = null,
        iframeUrl = "https://www.youtube.com/embed/abc?rel=0",
        publishedAt = 0L,
        views = 0L,
        hasSpoiler = false,
        category = BloggerVideoCategory("all", "All"),
        creator = Blogger(7, "blogger", null),
        reaction = reaction,
    )

    @Test
    fun `loads the video`() {
        val state = createViewModel().currentState

        assertFalse(state.loading)
        assertEquals("video", state.video?.title)
        assertEquals(USER_ID, state.currentUserId)
    }

    @Test
    fun `failed load shows the parsed error and retry recovers`() {
        coEvery { repository.getVideo(VIDEO_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("parsed message", vm.currentState.error)
        coEvery { repository.getVideo(VIDEO_ID) } returns video()

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals("video", vm.currentState.video?.title)
    }

    @Test
    fun `like is applied and replaced by the server reaction`() {
        coEvery { repository.setVideoVote(VIDEO_ID, BloggerVideoVote.LIKE) } returns
            BloggerVideoReaction(likes = 5, dislikes = 1, vote = BloggerVideoVote.LIKE)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(BloggerVideoVote.LIKE))

        val reaction = vm.currentState.video!!.reaction
        assertEquals(5, reaction.likes)
        assertEquals(BloggerVideoVote.LIKE, reaction.vote)
        assertFalse(vm.currentState.voting)
    }

    @Test
    fun `repeating the same vote cancels it`() {
        coEvery { repository.getVideo(VIDEO_ID) } returns
            video(BloggerVideoReaction(likes = 1, vote = BloggerVideoVote.LIKE))
        coEvery { repository.setVideoVote(VIDEO_ID, BloggerVideoVote.NONE) } returns BloggerVideoReaction(likes = 0)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(BloggerVideoVote.LIKE))

        coVerify(exactly = 1) { repository.setVideoVote(VIDEO_ID, BloggerVideoVote.NONE) }
        assertEquals(BloggerVideoVote.NONE, vm.currentState.video!!.reaction.vote)
    }

    @Test
    fun `failed vote rolls back to the confirmed reaction and shows a toast`() = runTest {
        coEvery { repository.setVideoVote(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(BloggerVideoVote.DISLIKE))

        assertEquals(BloggerVideoReaction(likes = 1, dislikes = 0), vm.currentState.video!!.reaction)
        assertFalse(vm.currentState.voting)
        assertEquals(listOf<Effect>(Effect.ShowToast("parsed message")), effects)
    }

    @Test
    fun `guest is asked to sign in instead of voting`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(BloggerVideoVote.LIKE))

        assertEquals(listOf<Effect>(Effect.ShowToast("auth required")), effects)
        coVerify(exactly = 0) { repository.setVideoVote(any(), any()) }
    }

    @Test
    fun `watch emits the youtube link`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.WatchSelected)

        assertEquals(listOf<Effect>(Effect.OpenVideo("https://www.youtube.com/watch?v=abc")), effects)
    }

    @Test
    fun `blogger and comments selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.BloggerSelected)
        vm.setEvent(Event.CommentsSelected)

        verify(exactly = 1) { navigator.blogger(7) }
        verify(exactly = 1) { commentsNavigator.getCommentsDest(CommentTargetType.BLOG_VIDEO, VIDEO_ID) }
        verify(exactly = 2) { nav.navigate(navKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val VIDEO_ID = 9
        const val USER_ID = 5
    }
}

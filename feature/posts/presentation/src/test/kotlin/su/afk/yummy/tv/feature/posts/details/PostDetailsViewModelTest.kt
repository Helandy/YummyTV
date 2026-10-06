package su.afk.yummy.tv.feature.posts.details

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
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.posts.model.PostAuthor
import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.domain.posts.model.PostReaction
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import su.afk.yummy.tv.domain.posts.usecase.GetPostDetailsUseCase
import su.afk.yummy.tv.domain.posts.usecase.RemovePostVoteUseCase
import su.afk.yummy.tv.domain.posts.usecase.VotePostUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.posts.details.PostDetailsState.Effect
import su.afk.yummy.tv.feature.posts.details.PostDetailsState.Event

class PostDetailsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val repository: PostsRepository = mockk()
    private val accountNavigator: IAccountNavigator = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val commentsNavigator: ICommentsNavigator = mockk()
    private val imageViewNavigator: IImageViewNavigator = mockk()
    private val strings: StringProvider = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val userId = MutableStateFlow(USER_ID)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { settingsStore.yaniUserId } returns userId
        every { accountNavigator.getUserProfileDest(any()) } returns navKey
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { commentsNavigator.getCommentsDest(any(), any()) } returns navKey
        every { imageViewNavigator(any(), any(), any(), any(), any(), any(), any(), any()) } returns navKey
        coEvery { repository.details(POST_ID) } returns details()
    }

    private fun createViewModel() = PostDetailsViewModel(
        postId = POST_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        getPostDetails = GetPostDetailsUseCase(repository),
        votePost = VotePostUseCase(repository),
        removePostVote = RemovePostVoteUseCase(repository),
        accountNavigator = accountNavigator,
        detailsNavigator = detailsNavigator,
        commentsNavigator = commentsNavigator,
        imageViewNavigator = imageViewNavigator,
        strings = strings,
        settingsStore = settingsStore,
    )

    private fun details(vote: PostVote = PostVote.NONE) = PostDetails(
        id = POST_ID,
        title = "Post",
        contentHtml = "",
        previewImageUrl = "https://img/preview.jpg",
        author = PostAuthor(1, "author", null),
        category = PostCategory(1, "News", "news"),
        createdAt = 0L,
        editedAt = null,
        relatedAnime = emptyList(),
        reaction = PostReaction(likes = 1, dislikes = 0, vote = vote),
        views = 0,
        comments = 0,
    )

    @Test
    fun `loads the post`() {
        val state = createViewModel().currentState

        assertFalse(state.loading)
        assertEquals("Post", state.details?.title)
        assertEquals(USER_ID, state.currentUserId)
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { repository.details(POST_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("parsed message", vm.currentState.error)
        coEvery { repository.details(POST_ID) } returns details()

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals("Post", vm.currentState.details?.title)
    }

    @Test
    fun `like is saved and replaced by the server reaction`() {
        coEvery { repository.vote(POST_ID, PostVote.LIKE) } returns PostReaction(5, 0, PostVote.LIKE)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(PostVote.LIKE))

        assertEquals(5, vm.currentState.details?.reaction?.likes)
        assertFalse(vm.currentState.voting)
    }

    @Test
    fun `repeating the same vote removes it`() {
        coEvery { repository.details(POST_ID) } returns details(PostVote.LIKE)
        coEvery { repository.removeVote(POST_ID) } returns PostReaction(0, 0, PostVote.NONE)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(PostVote.LIKE))

        coVerify(exactly = 1) { repository.removeVote(POST_ID) }
        assertEquals(PostVote.NONE, vm.currentState.details?.reaction?.vote)
    }

    @Test
    fun `failed vote restores the reaction and shows a toast`() = runTest {
        coEvery { repository.vote(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        val original = vm.currentState.details!!.reaction

        vm.setEvent(Event.VoteSelected(PostVote.LIKE))

        assertEquals(original, vm.currentState.details?.reaction)
        assertFalse(vm.currentState.voting)
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `guest vote shows the auth toast`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(PostVote.LIKE))

        assertEquals(1, effects.size)
        coVerify(exactly = 0) { repository.vote(any(), any()) }
    }

    @Test
    fun `image opens the gallery of the post images`() {
        val vm = createViewModel()

        vm.setEvent(Event.ImageSelected("https://img/preview.jpg"))

        verify(exactly = 1) {
            imageViewNavigator(
                imageUrl = "https://img/preview.jpg",
                imageUrls = listOf("https://img/preview.jpg"),
                selectedIndex = 0,
                service = any(),
                creatorName = any(),
                postId = any(),
                postTitle = any(),
                thumbnailUrls = any(),
            )
        }
        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `anime author and comments selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(4))
        vm.setEvent(Event.AuthorSelected(3))
        vm.setEvent(Event.CommentsSelected)

        verify(exactly = 1) { detailsNavigator.getDetailsDest(4) }
        verify(exactly = 1) { accountNavigator.getUserProfileDest(3) }
        verify(exactly = 1) { commentsNavigator.getCommentsDest(CommentTargetType.POST, POST_ID) }
        verify(exactly = 3) { nav.navigate(navKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val POST_ID = 6
        const val USER_ID = 5
    }
}

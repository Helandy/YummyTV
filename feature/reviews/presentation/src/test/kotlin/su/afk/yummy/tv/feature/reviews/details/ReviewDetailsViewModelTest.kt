package su.afk.yummy.tv.feature.reviews.details

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
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewMutationRepository
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import su.afk.yummy.tv.domain.reviews.usecase.DeleteReviewUseCase
import su.afk.yummy.tv.domain.reviews.usecase.GetReviewDetailsUseCase
import su.afk.yummy.tv.domain.reviews.usecase.VoteReviewUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsState.Effect
import su.afk.yummy.tv.feature.reviews.details.ReviewDetailsState.Event
import su.afk.yummy.tv.feature.reviews.reviewDetails

class ReviewDetailsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val commentsNavigator: ICommentsNavigator = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val repository: ReviewsRepository = mockk()
    private val mutationNotifier: ReviewMutationRepository = mockk(relaxed = true)
    private val strings: StringProvider = mockk()
    private val pendingMutationQueue: PendingMutationQueue = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val userId = MutableStateFlow(OWNER_ID)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { settingsStore.yaniUserId } returns userId
        every { accountNavigator.getUserProfileDest(any()) } returns navKey
        every { commentsNavigator.getCommentsDest(any(), any()) } returns navKey
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        coEvery { repository.getReview(REVIEW_ID) } returns reviewDetails(REVIEW_ID, authorId = OWNER_ID)
    }

    private fun createViewModel() = ReviewDetailsViewModel(
        reviewId = REVIEW_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        commentsNavigator = commentsNavigator,
        detailsNavigator = detailsNavigator,
        getReviewDetails = GetReviewDetailsUseCase(repository),
        voteReview = VoteReviewUseCase(repository, mutationNotifier),
        deleteReview = DeleteReviewUseCase(repository, mutationNotifier),
        strings = strings,
        pendingMutationQueue = pendingMutationQueue,
        settingsStore = settingsStore,
    )

    @Test
    fun `loads the review and detects the owner`() {
        val state = createViewModel().currentState

        assertFalse(state.loading)
        assertEquals(REVIEW_ID, state.details?.review?.id)
        assertTrue(state.isOwner)
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { repository.getReview(REVIEW_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("parsed message", vm.currentState.error)
        coEvery { repository.getReview(REVIEW_ID) } returns reviewDetails(REVIEW_ID)

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(REVIEW_ID, vm.currentState.details?.review?.id)
    }

    @Test
    fun `vote is replaced by the saved reactions`() {
        val saved = ReviewReactions(likes = 7, dislikes = 0, vote = ReviewVote.LIKE)
        coEvery { repository.vote(REVIEW_ID, ReviewVote.LIKE) } returns saved
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(ReviewVote.LIKE))

        assertEquals(saved, vm.currentState.details?.review?.reactions)
    }

    @Test
    fun `failed vote that cannot be queued rolls back and shows a toast`() = runTest {
        coEvery { repository.vote(any(), any()) } throws IllegalStateException("boom")
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), any()) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        val original = vm.currentState.details!!.review.reactions

        vm.setEvent(Event.VoteSelected(ReviewVote.LIKE))

        assertEquals(original, vm.currentState.details?.review?.reactions)
        assertEquals(1, effects.size)
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `guest vote shows the auth toast`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(ReviewVote.LIKE))

        assertEquals(1, effects.size)
        coVerify(exactly = 0) { repository.vote(any(), any()) }
    }

    @Test
    fun `owner deletes the review and leaves`() = runTest {
        coEvery { repository.delete(REVIEW_ID) } returns true
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.DeleteConfirmed)

        assertEquals(listOf<Effect>(Effect.Deleted), effects)
        verify(exactly = 1) { nav.back() }
        assertFalse(vm.currentState.deleting)
    }

    @Test
    fun `not deleted review shows a toast and stays`() = runTest {
        coEvery { repository.delete(REVIEW_ID) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.DeleteConfirmed)

        assertEquals(1, effects.size)
        assertTrue(effects.single() is Effect.ShowToast)
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `non owner cannot delete`() {
        coEvery { repository.getReview(REVIEW_ID) } returns reviewDetails(REVIEW_ID, authorId = 999)
        val vm = createViewModel()

        vm.setEvent(Event.DeleteConfirmed)

        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `author anime and comments selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.AuthorSelected(3))
        vm.setEvent(Event.AnimeSelected(4))
        vm.setEvent(Event.CommentsSelected)

        verify(exactly = 1) { accountNavigator.getUserProfileDest(3) }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(4) }
        verify(exactly = 1) { commentsNavigator.getCommentsDest(CommentTargetType.REVIEW, REVIEW_ID) }
        verify(exactly = 3) { nav.navigate(navKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val REVIEW_ID = 2
        const val OWNER_ID = 10
    }
}

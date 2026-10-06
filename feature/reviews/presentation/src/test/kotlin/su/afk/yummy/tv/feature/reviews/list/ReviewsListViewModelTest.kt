package su.afk.yummy.tv.feature.reviews.list

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
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewMutationRepository
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import su.afk.yummy.tv.domain.reviews.usecase.GetAnimeReviewsUseCase
import su.afk.yummy.tv.domain.reviews.usecase.GetReviewFeedUseCase
import su.afk.yummy.tv.domain.reviews.usecase.VoteReviewUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.reviews.list.ReviewsListState.Effect
import su.afk.yummy.tv.feature.reviews.list.ReviewsListState.Event
import su.afk.yummy.tv.feature.reviews.reviewSummary

class ReviewsListViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val navigator: IReviewsNavigator = mockk()
    private val accountNavigator: IAccountNavigator = mockk()
    private val repository: ReviewsRepository = mockk()
    private val strings: StringProvider = mockk()
    private val pendingMutationQueue: PendingMutationQueue = mockk()
    private val mutationNotifier: ReviewMutationRepository = mockk(relaxed = true)
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val userId = MutableStateFlow(USER_ID)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { mutationNotifier.version } returns MutableStateFlow(0L)
        every { settingsStore.yaniUserId } returns userId
        every { navigator.details(any()) } returns navKey
        every { accountNavigator.getUserProfileDest(any()) } returns navKey
    }

    private fun createViewModel(animeId: Int? = null) = ReviewsListViewModel(
        animeId = animeId,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        navigator = navigator,
        accountNavigator = accountNavigator,
        getReviewFeed = GetReviewFeedUseCase(repository),
        getAnimeReviews = GetAnimeReviewsUseCase(repository),
        voteReview = VoteReviewUseCase(repository, mutationNotifier),
        strings = strings,
        pendingMutationQueue = pendingMutationQueue,
        mutationNotifier = mutationNotifier,
        settingsStore = settingsStore,
    )

    @Test
    fun `general feed offers only new and top sorting`() {
        val state = createViewModel().currentState

        assertTrue(state.isGeneralFeed)
        assertEquals(listOf(ReviewSort.NEW, ReviewSort.TOP), state.availableSorts)
        assertEquals(USER_ID, state.currentUserId)
    }

    @Test
    fun `anime reviews offer every sorting`() {
        val state = createViewModel(animeId = 5).currentState

        assertFalse(state.isGeneralFeed)
        assertEquals(ReviewSort.entries, state.availableSorts)
    }

    @Test
    fun `new sort rebuilds the flow and the same sort is ignored`() {
        val vm = createViewModel()
        val before = vm.currentState.reviews

        vm.setEvent(Event.SortSelected(ReviewSort.NEW))
        assertTrue(before === vm.currentState.reviews)

        vm.setEvent(Event.SortSelected(ReviewSort.TOP))
        assertEquals(ReviewSort.TOP, vm.currentState.sort)
        assertNotSame(before, vm.currentState.reviews)
    }

    @Test
    fun `vote is applied optimistically and replaced by the saved reactions`() {
        val saved = ReviewReactions(likes = 9, dislikes = 0, vote = ReviewVote.LIKE)
        coEvery { repository.vote(1, ReviewVote.LIKE) } returns saved
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(reviewSummary(id = 1), ReviewVote.LIKE))

        assertEquals(saved, vm.currentState.reactionOverrides[1])
        verify(exactly = 1) { mutationNotifier.notifyChanged() }
    }

    @Test
    fun `network failure keeps the optimistic vote and queues it`() {
        val error = IllegalStateException("offline")
        coEvery { repository.vote(any(), any()) } throws error
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), any()) } returns true
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(reviewSummary(id = 1), ReviewVote.LIKE))

        assertEquals(ReviewVote.LIKE, vm.currentState.reactionOverrides[1]?.vote)
        coVerify(exactly = 1) { pendingMutationQueue.enqueueOnNetworkFailure(PendingMutation.VoteReview(1, 1), error) }
    }

    @Test
    fun `other failure rolls the vote back and shows a toast`() = runTest {
        val original = ReviewReactions(likes = 1, dislikes = 0, vote = ReviewVote.NONE)
        coEvery { repository.vote(any(), any()) } throws IllegalStateException("boom")
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), any()) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(reviewSummary(id = 1, reactions = original), ReviewVote.LIKE))

        assertEquals(original, vm.currentState.reactionOverrides[1])
        assertEquals(1, effects.size)
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `guest vote shows the auth toast`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(reviewSummary(), ReviewVote.LIKE))

        assertEquals(1, effects.size)
        coVerify(exactly = 0) { repository.vote(any(), any()) }
    }

    @Test
    fun `review and author selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.ReviewSelected(4))
        vm.setEvent(Event.AuthorSelected(10))
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { navigator.details(4) }
        verify(exactly = 1) { nav.navigateDetail(navKey) }
        verify(exactly = 1) { accountNavigator.getUserProfileDest(10) }
        verify(exactly = 1) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val USER_ID = 5
    }
}

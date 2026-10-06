package su.afk.yummy.tv.feature.collection

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.domain.collection.model.CollectionVoteResult
import su.afk.yummy.tv.domain.collection.model.UpdateCollectionRequest
import su.afk.yummy.tv.domain.collection.repository.CollectionMutationRepository
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import su.afk.yummy.tv.domain.collection.usecase.DeleteCollectionUseCase
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionUseCase
import su.afk.yummy.tv.domain.collection.usecase.RemoveCollectionVoteUseCase
import su.afk.yummy.tv.domain.collection.usecase.UpdateCollectionUseCase
import su.afk.yummy.tv.domain.collection.usecase.VoteCollectionUseCase
import su.afk.yummy.tv.feature.collection.CollectionState.Event
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class CollectionViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val commentsNavigator: ICommentsNavigator = mockk()
    private val repository: CollectionRepository = mockk()
    private val mutationNotifier: CollectionMutationRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk()
    private val stringProvider: StringProvider = mockk()
    private val analytics: CollectionAnalytics = mockk(relaxed = true)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { commentsNavigator.getCommentsDest(any(), any()) } returns navKey
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = true, userId = OWNER_ID)
        coEvery { repository.getCollection(COLLECTION_ID) } returns collection()
    }

    private fun createViewModel() = CollectionViewModel(
        collectionId = COLLECTION_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        commentsNavigator = commentsNavigator,
        getCollection = GetCollectionUseCase(repository),
        getAccountSession = GetAccountSessionUseCase(accountRepository),
        voteCollection = VoteCollectionUseCase(repository),
        removeCollectionVote = RemoveCollectionVoteUseCase(repository),
        updateCollection = UpdateCollectionUseCase(repository, mutationNotifier),
        deleteCollection = DeleteCollectionUseCase(repository, mutationNotifier),
        stringProvider = stringProvider,
        analytics = analytics,
    )

    private fun collection(vote: CollectionVote = CollectionVote.NEUTRAL) = CollectionDetail(
        id = COLLECTION_ID,
        ownerId = OWNER_ID,
        title = "Title",
        description = "About",
        isPublic = true,
        views = 1,
        posterUrl = null,
        likesCount = 2,
        dislikesCount = 1,
        vote = vote,
        animes = emptyList(),
    )

    @Test
    fun `loads the collection and detects the owner`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("Title", state.collection?.title)
        assertTrue(state.isOwner)
        verify(exactly = 1) { analytics.eventScreenOpened(COLLECTION_ID) }
    }

    @Test
    fun `guest is not the owner`() {
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = false, userId = 0)

        val state = createViewModel().currentState

        assertFalse(state.isOwner)
    }

    @Test
    fun `failed load shows the message and retry recovers`() {
        coEvery { repository.getCollection(COLLECTION_ID) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("boom", vm.currentState.error)
        verify(exactly = 1) { analytics.eventLoadError(any()) }
        coEvery { repository.getCollection(COLLECTION_ID) } returns collection()

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertNotNull(vm.currentState.collection)
    }

    @Test
    fun `like stores the new counters and vote`() {
        coEvery { repository.voteCollection(COLLECTION_ID, CollectionVote.LIKE) } returns CollectionVoteResult(3, 1)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(CollectionVote.LIKE))

        assertEquals(3, vm.currentState.collection?.likesCount)
        assertEquals(CollectionVote.LIKE, vm.currentState.collection?.vote)
        assertFalse(vm.currentState.isVoteLoading)
    }

    @Test
    fun `repeating the same vote removes it`() {
        coEvery { repository.getCollection(COLLECTION_ID) } returns collection(CollectionVote.LIKE)
        coEvery { repository.removeCollectionVote(COLLECTION_ID) } returns CollectionVoteResult(1, 1)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(CollectionVote.LIKE))

        assertEquals(CollectionVote.NEUTRAL, vm.currentState.collection?.vote)
        coVerify(exactly = 1) { repository.removeCollectionVote(COLLECTION_ID) }
    }

    @Test
    fun `neutral vote is ignored`() {
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(CollectionVote.NEUTRAL))

        coVerify(exactly = 0) { repository.voteCollection(any(), any()) }
    }

    @Test
    fun `guest vote shows the auth toast`() = runTest {
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(CollectionVote.LIKE))

        assertEquals(1, effects.size)
        coVerify(exactly = 0) { repository.voteCollection(any(), any()) }
    }

    @Test
    fun `failed vote shows a toast`() = runTest {
        coEvery { repository.voteCollection(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(CollectionVote.LIKE))

        assertEquals(1, effects.size)
        assertFalse(vm.currentState.isVoteLoading)
    }

    @Test
    fun `owner edits the collection`() {
        coEvery { repository.updateCollection(COLLECTION_ID, any()) } returns true
        val vm = createViewModel()
        vm.setEvent(Event.EditSelected)
        assertTrue(vm.currentState.isEditDialogVisible)
        assertEquals("Title", vm.currentState.editTitle)
        vm.setEvent(Event.EditTitleChanged(" New "))
        vm.setEvent(Event.EditDescriptionChanged(" Text "))
        vm.setEvent(Event.EditPublicChanged(false))

        vm.setEvent(Event.EditConfirmed)

        coVerify(exactly = 1) {
            repository.updateCollection(COLLECTION_ID, UpdateCollectionRequest("New", "Text", false))
        }
        assertFalse(vm.currentState.isEditDialogVisible)
        assertEquals("New", vm.currentState.collection?.title)
    }

    @Test
    fun `rejected update shows a toast and keeps the dialog`() = runTest {
        coEvery { repository.updateCollection(any(), any()) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.EditSelected)

        vm.setEvent(Event.EditConfirmed)

        assertTrue(vm.currentState.isEditDialogVisible)
        assertFalse(vm.currentState.isUpdating)
        assertEquals(1, effects.size)
    }

    @Test
    fun `guest cannot open the edit or delete dialogs`() {
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()

        vm.setEvent(Event.EditSelected)
        vm.setEvent(Event.DeleteSelected)

        assertFalse(vm.currentState.isEditDialogVisible)
        assertFalse(vm.currentState.isDeleteDialogVisible)
    }

    @Test
    fun `confirmed delete removes the collection and leaves the screen`() {
        coEvery { repository.deleteCollection(COLLECTION_ID) } returns true
        val vm = createViewModel()
        vm.setEvent(Event.DeleteSelected)
        assertTrue(vm.currentState.isDeleteDialogVisible)

        vm.setEvent(Event.DeleteConfirmed)

        coVerify(exactly = 1) { repository.deleteCollection(COLLECTION_ID) }
        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `failed delete stays and shows a toast`() = runTest {
        coEvery { repository.deleteCollection(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.DeleteSelected)

        vm.setEvent(Event.DeleteConfirmed)

        assertFalse(vm.currentState.isDeleting)
        assertEquals(1, effects.size)
        verify(exactly = 0) { nav.back() }
    }

    @Test
    fun `delete dialog can be dismissed`() {
        val vm = createViewModel()
        vm.setEvent(Event.DeleteSelected)

        vm.setEvent(Event.DeleteDismissed)

        assertFalse(vm.currentState.isDeleteDialogVisible)
    }

    @Test
    fun `anime and comments selections navigate and grid scroll is remembered`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(7))
        vm.setEvent(Event.CommentsSelected)
        vm.setEvent(Event.GridScrolled(index = 4, offset = 12))

        verify(exactly = 1) { detailsNavigator.getDetailsDest(7) }
        verify(exactly = 1) { commentsNavigator.getCommentsDest(CommentTargetType.COLLECTION, COLLECTION_ID) }
        verify(exactly = 2) { nav.navigate(navKey) }
        assertEquals(4, vm.currentState.firstVisibleItemIndex)
        assertEquals(12, vm.currentState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val COLLECTION_ID = 3
        const val OWNER_ID = 8
    }
}

package su.afk.yummy.tv.feature.comments

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentAuthor
import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentVoteResult
import su.afk.yummy.tv.domain.comments.model.CommentsPage
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import su.afk.yummy.tv.domain.comments.usecase.GetCommentChildrenUseCase
import su.afk.yummy.tv.domain.comments.usecase.GetCommentsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.CommentsState.CommentUi
import su.afk.yummy.tv.feature.comments.CommentsState.ComposerMode
import su.afk.yummy.tv.feature.comments.CommentsState.Effect
import su.afk.yummy.tv.feature.comments.CommentsState.Event
import su.afk.yummy.tv.feature.comments.handler.CommentVoteChange
import su.afk.yummy.tv.feature.comments.handler.CommentsMutationHandler

class CommentsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val stringProvider: StringProvider = mockk()
    private val repository: CommentsRepository = mockk()
    private val mutationHandler: CommentsMutationHandler = mockk()
    private val analytics: CommentsAnalytics = mockk(relaxed = true)
    private val userId = MutableStateFlow(ME_ID)
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { settingsStore.yaniUserId } returns userId
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { accountNavigator.getUserProfileDest(any()) } returns navKey
    }

    private fun createViewModel() = CommentsViewModel(
        targetType = CommentTargetType.POST,
        targetId = TARGET_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        settingsStore = settingsStore,
        stringProvider = stringProvider,
        getComments = GetCommentsUseCase(repository),
        getCommentChildren = GetCommentChildrenUseCase(repository),
        mutationHandler = mutationHandler,
        analytics = analytics,
    )

    private fun comment(
        id: Int,
        authorId: Int = ME_ID,
        parentId: Int? = null,
        vote: CommentVote = CommentVote.NEUTRAL,
    ) = Comment(
        id = id,
        author = CommentAuthor(authorId, "author$authorId", null, null, null),
        text = "text $id",
        createdAtEpochSeconds = 0L,
        parentId = parentId,
        childrenCount = 0,
        likes = 0,
        dislikes = 0,
        vote = vote,
        roles = emptyList(),
        deletedAtEpochSeconds = null,
    )

    /** Показывает во ViewModel комментарии так, как это делает список на экране. */
    private fun CommentsViewModel.show(vararg comments: Comment) =
        setEvent(Event.VisibleCommentsChanged(comments.map { CommentUi(it) }))

    @Test
    fun `starts with the best sorting and tracks the screen`() {
        val state = createViewModel().currentState

        assertEquals(CommentSort.BEST, state.sort)
        assertEquals(ME_ID, state.currentUserId)
        assertTrue(state.isSignedIn)
        verify(exactly = 1) { analytics.eventScreenOpened(any(), CommentSort.BEST) }
    }

    @Test
    fun `new sort rebuilds the comments flow and resets the local changes`() {
        val vm = createViewModel()
        val before = vm.currentState.comments

        vm.setEvent(Event.SortSelected(CommentSort.NEW))

        assertEquals(CommentSort.NEW, vm.currentState.sort)
        assertNotSame(before, vm.currentState.comments)
    }

    @Test
    fun `same sort changes nothing`() {
        val vm = createViewModel()
        val before = vm.currentState.comments

        vm.setEvent(Event.SortSelected(CommentSort.BEST))

        assertTrue(before === vm.currentState.comments)
    }

    @Test
    fun `refresh and retry rebuild the flow`() {
        val vm = createViewModel()
        val first = vm.currentState.comments

        vm.setEvent(Event.RefreshSelected)
        val second = vm.currentState.comments
        vm.setEvent(Event.RetrySelected)

        assertNotSame(first, second)
        assertNotSame(second, vm.currentState.comments)
    }

    @Test
    fun `composer text is kept and can be cancelled`() {
        val vm = createViewModel()

        vm.setEvent(Event.ComposerTextChanged("hello"))
        assertEquals("hello", vm.currentState.composerText)
        vm.setEvent(Event.ComposerCancelled)

        assertEquals("", vm.currentState.composerText)
        assertEquals(ComposerMode.New, vm.currentState.composerMode)
    }

    @Test
    fun `guest cannot submit and gets an auth toast`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.ComposerTextChanged("hello"))

        vm.setEvent(Event.SubmitSelected)

        assertEquals(1, effects.size)
        coVerify(exactly = 0) { mutationHandler.create(any(), any(), any()) }
    }

    @Test
    fun `blank text shows the empty text toast`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.SubmitSelected)

        assertEquals(1, effects.size)
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `new comment is prepended and the composer is cleared`() {
        coEvery { mutationHandler.create(CommentTargetType.POST, TARGET_ID, CommentDraft("hello")) } returns
            Result.success(comment(10))
        val vm = createViewModel()
        vm.setEvent(Event.ComposerTextChanged(" hello "))

        vm.setEvent(Event.SubmitSelected)

        assertEquals(listOf(10), vm.currentState.prependedComments.map { it.comment.id })
        assertEquals("", vm.currentState.composerText)
        assertFalse(vm.currentState.isMutating)
    }

    @Test
    fun `failed submit shows a toast and keeps the text`() = runTest {
        coEvery { mutationHandler.create(any(), any(), any()) } returns Result.failure(IllegalStateException("boom"))
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.ComposerTextChanged("hello"))

        vm.setEvent(Event.SubmitSelected)

        assertEquals("hello", vm.currentState.composerText)
        assertFalse(vm.currentState.isMutating)
        assertEquals(listOf<Effect>(Effect.ShowToast("boom")), effects)
    }

    @Test
    fun `reply to a reply targets the root comment`() {
        val vm = createViewModel()
        vm.show(comment(1, authorId = OTHER_ID), comment(2, authorId = OTHER_ID, parentId = 1))

        vm.setEvent(Event.ReplySelected(2))

        val mode = vm.currentState.composerMode as ComposerMode.Reply
        assertEquals(1, mode.parentCommentId)
        assertEquals(2, mode.replyToCommentId)
    }

    @Test
    fun `reply is sent with parent and loads the replies`() {
        coEvery { mutationHandler.create(any(), any(), any()) } returns Result.success(comment(20, parentId = 1))
        coEvery { repository.getCommentChildren(1, 0) } returns CommentsPage(listOf(comment(20, parentId = 1)), false)
        val vm = createViewModel()
        vm.show(comment(1, authorId = OTHER_ID))
        vm.setEvent(Event.ReplySelected(1))
        vm.setEvent(Event.ComposerTextChanged("answer"))

        vm.setEvent(Event.SubmitSelected)

        coVerify(exactly = 1) {
            mutationHandler.create(
                CommentTargetType.POST,
                TARGET_ID,
                CommentDraft(text = "answer", parentCommentId = 1, replyToCommentId = 1),
            )
        }
        assertEquals(ComposerMode.New, vm.currentState.composerMode)
        assertEquals(listOf(20), vm.currentState.commentOverlays[1]?.children?.map { it.comment.id })
    }

    @Test
    fun `own comment can be edited and saved`() {
        coEvery { mutationHandler.update(5, "fixed") } returns Result.success(comment(5).copy(text = "fixed"))
        val vm = createViewModel()
        vm.show(comment(5))

        vm.setEvent(Event.EditSelected(5))
        assertEquals(ComposerMode.Edit(5), vm.currentState.composerMode)
        assertEquals("text 5", vm.currentState.composerText)
        vm.setEvent(Event.ComposerTextChanged("fixed"))
        vm.setEvent(Event.SubmitSelected)

        assertEquals("fixed", vm.currentState.commentOverlays[5]?.comment?.text)
        assertEquals(ComposerMode.New, vm.currentState.composerMode)
    }

    @Test
    fun `foreign comment cannot be edited`() = runTest {
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.show(comment(5, authorId = OTHER_ID))

        vm.setEvent(Event.EditSelected(5))

        assertEquals(ComposerMode.New, vm.currentState.composerMode)
        assertEquals(1, effects.size)
    }

    @Test
    fun `confirmed delete hides the comment`() {
        coEvery { mutationHandler.delete(5) } returns Result.success(true)
        val vm = createViewModel()
        vm.show(comment(5))
        vm.setEvent(Event.DeleteSelected(5))
        assertEquals(5, vm.currentState.pendingDelete?.id)

        vm.setEvent(Event.DeleteConfirmed)

        assertTrue(5 in vm.currentState.deletedCommentIds)
        assertNull(vm.currentState.pendingDelete)
    }

    @Test
    fun `refused delete keeps the dialog and shows a toast`() = runTest {
        coEvery { mutationHandler.delete(5) } returns Result.success(false)
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.show(comment(5))
        vm.setEvent(Event.DeleteSelected(5))

        vm.setEvent(Event.DeleteConfirmed)

        assertEquals(5, vm.currentState.pendingDelete?.id)
        assertEquals(1, effects.size)
    }

    @Test
    fun `delete dialog can be dismissed`() {
        val vm = createViewModel()
        vm.show(comment(5))
        vm.setEvent(Event.DeleteSelected(5))

        vm.setEvent(Event.DeleteDismissed)

        assertNull(vm.currentState.pendingDelete)
    }

    @Test
    fun `report is sent with the chosen reason`() = runTest {
        coEvery { mutationHandler.report(5, CommentReportReason.SPAM) } returns Result.success(true)
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.show(comment(5, authorId = OTHER_ID))
        vm.setEvent(Event.ReportSelected(5))
        assertEquals(5, vm.currentState.pendingReport?.id)

        vm.setEvent(Event.ReportConfirmed(CommentReportReason.SPAM))

        assertNull(vm.currentState.pendingReport)
        assertEquals(1, effects.size)
    }

    @Test
    fun `vote updates the comment through an overlay`() {
        coEvery { mutationHandler.changeVote(5, CommentVote.NEUTRAL, CommentVote.LIKE) } returns
            Result.success(CommentVoteChange(CommentVoteResult(likes = 3, dislikes = 0, success = true), CommentVote.LIKE))
        val vm = createViewModel()
        vm.show(comment(5, authorId = OTHER_ID))

        vm.setEvent(Event.VoteSelected(5, CommentVote.LIKE))

        val updated = vm.currentState.commentOverlays[5]!!.comment
        assertEquals(3, updated.likes)
        assertEquals(CommentVote.LIKE, updated.vote)
    }

    @Test
    fun `neutral vote and guest vote are ignored`() {
        val vm = createViewModel()
        vm.show(comment(5, authorId = OTHER_ID))
        vm.setEvent(Event.VoteSelected(5, CommentVote.NEUTRAL))
        userId.value = 0

        vm.setEvent(Event.VoteSelected(5, CommentVote.LIKE))

        coVerify(exactly = 0) { mutationHandler.changeVote(any(), any(), any()) }
    }

    @Test
    fun `failed vote shows a toast`() = runTest {
        coEvery { mutationHandler.changeVote(any(), any(), any()) } returns Result.failure(IllegalStateException("boom"))
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.show(comment(5, authorId = OTHER_ID))

        vm.setEvent(Event.VoteSelected(5, CommentVote.LIKE))

        assertEquals(1, effects.size)
        assertTrue(vm.currentState.commentOverlays.isEmpty())
    }

    @Test
    fun `children are loaded on the first toggle and then only hidden and shown`() {
        coEvery { repository.getCommentChildren(1, 0) } returns CommentsPage(listOf(comment(2, parentId = 1)), isModerator = true)
        val vm = createViewModel()
        vm.show(comment(1, authorId = OTHER_ID))

        vm.setEvent(Event.ChildrenToggleSelected(1))
        val loaded = vm.currentState.commentOverlays[1]!!
        assertTrue(loaded.childrenVisible)
        assertEquals(listOf(2), loaded.children.map { it.comment.id })
        assertTrue(vm.currentState.isModerator)

        vm.setEvent(Event.VisibleCommentsChanged(listOf(loaded)))
        vm.setEvent(Event.ChildrenToggleSelected(1))
        assertFalse(vm.currentState.commentOverlays[1]!!.childrenVisible)

        vm.setEvent(Event.ChildrenToggleSelected(1))
        assertTrue(vm.currentState.commentOverlays[1]!!.childrenVisible)
        coVerify(exactly = 1) { repository.getCommentChildren(1, 0) }
    }

    @Test
    fun `failed children load stores the error on the comment`() {
        coEvery { repository.getCommentChildren(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        vm.show(comment(1, authorId = OTHER_ID))

        vm.setEvent(Event.ChildrenToggleSelected(1))

        val ui = vm.currentState.commentOverlays[1]!!
        assertEquals("boom", ui.childrenError)
        assertFalse(ui.childrenLoading)
    }

    @Test
    fun `author selection opens the profile only for a valid id`() {
        val vm = createViewModel()

        vm.setEvent(Event.AuthorSelected(OTHER_ID))
        vm.setEvent(Event.AuthorSelected(0))
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { accountNavigator.getUserProfileDest(OTHER_ID) }
        verify(exactly = 1) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val TARGET_ID = 7
        const val ME_ID = 1
        const val OTHER_ID = 2
    }
}

package su.afk.yummy.tv.feature.comments.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentVoteResult
import su.afk.yummy.tv.domain.comments.usecase.AddCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.DeleteCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.RemoveCommentVoteUseCase
import su.afk.yummy.tv.domain.comments.usecase.ReportCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.UpdateCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.VoteCommentUseCase

class CommentsMutationHandlerTest : BaseUnitTest() {

    private val voteComment: VoteCommentUseCase = mockk()
    private val removeCommentVote: RemoveCommentVoteUseCase = mockk()
    private val deleteComment: DeleteCommentUseCase = mockk()

    private val result = CommentVoteResult(likes = 3, dislikes = 1, success = true)

    @Test
    fun `pressing the active vote again removes it`() = runTest {
        coEvery { removeCommentVote(COMMENT_ID) } returns result

        val change = createHandler().changeVote(COMMENT_ID, CommentVote.LIKE, CommentVote.LIKE).getOrThrow()

        assertEquals(CommentVoteChange(result, CommentVote.NEUTRAL), change)
        coVerify(exactly = 0) { voteComment(any(), any()) }
    }

    @Test
    fun `another vote replaces the current one`() = runTest {
        coEvery { voteComment(COMMENT_ID, CommentVote.DISLIKE) } returns result

        val change = createHandler().changeVote(COMMENT_ID, CommentVote.LIKE, CommentVote.DISLIKE).getOrThrow()

        assertEquals(CommentVoteChange(result, CommentVote.DISLIKE), change)
    }

    @Test
    fun `failures are returned instead of thrown`() = runTest {
        coEvery { voteComment(any(), any()) } throws IllegalStateException("500")
        coEvery { deleteComment(any()) } throws IllegalStateException("403")
        val handler = createHandler()

        assertTrue(handler.changeVote(COMMENT_ID, CommentVote.NEUTRAL, CommentVote.LIKE).isFailure)
        assertTrue(handler.delete(COMMENT_ID).isFailure)
    }

    private fun createHandler() = CommentsMutationHandler(
        addComment = mockk<AddCommentUseCase>(),
        updateComment = mockk<UpdateCommentUseCase>(),
        deleteComment = deleteComment,
        voteComment = voteComment,
        removeCommentVote = removeCommentVote,
        reportComment = mockk<ReportCommentUseCase>(),
    )

    private companion object {
        const val COMMENT_ID = 7
    }
}

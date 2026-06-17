package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

class VoteCommentUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(commentId: Int, vote: CommentVote) =
        repository.voteComment(commentId, vote)
}

package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

class UpdateCommentUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(commentId: Int, text: String) =
        repository.updateComment(commentId, text)
}

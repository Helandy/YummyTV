package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

/** Удаляет реакцию текущего пользователя с выбранного комментария. */
class RemoveCommentVoteUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(commentId: Int) =
        repository.removeCommentVote(commentId)
}

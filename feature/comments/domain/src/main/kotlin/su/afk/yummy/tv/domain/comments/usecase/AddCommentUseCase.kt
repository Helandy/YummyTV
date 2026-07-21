package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

/** Добавляет комментарий или ответ к выбранной сущности. */
class AddCommentUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(
        targetType: CommentTargetType,
        targetId: Int,
        draft: CommentDraft,
    ) = repository.addComment(targetType, targetId, draft)
}

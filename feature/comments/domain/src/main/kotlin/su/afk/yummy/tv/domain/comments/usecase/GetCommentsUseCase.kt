package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(
        targetType: CommentTargetType,
        targetId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
        forceRefresh: Boolean = false,
    ) = repository.getComments(targetType, targetId, limit, skip, sort, forceRefresh)
}

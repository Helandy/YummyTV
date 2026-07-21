package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

/** Загружает страницу дочерних ответов для выбранного комментария. */
class GetCommentChildrenUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(commentId: Int, skip: Int) =
        repository.getCommentChildren(commentId, skip)
}

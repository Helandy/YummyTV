package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

class GetAnimeCommentsUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(
        animeId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
        forceRefresh: Boolean = false,
    ) = repository.getAnimeComments(animeId, limit, skip, sort, forceRefresh)
}

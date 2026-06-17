package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

class AddAnimeCommentUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(animeId: Int, draft: CommentDraft) =
        repository.addAnimeComment(animeId, draft)
}

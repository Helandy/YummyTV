package su.afk.yummy.tv.domain.comments.usecase

import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository
import javax.inject.Inject

/** Отправляет жалобу на выбранный комментарий с указанной причиной. */
class ReportCommentUseCase @Inject constructor(
    private val repository: CommentsRepository,
) {
    suspend operator fun invoke(commentId: Int, reason: CommentReportReason) =
        repository.reportComment(commentId, reason)
}

package su.afk.yummy.tv.domain.reviews.usecase

import su.afk.yummy.tv.domain.reviews.ReviewMutationNotifier
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import javax.inject.Inject

/** Сохраняет реакцию на рецензию и уведомляет наблюдателей об изменении списка. */
class VoteReviewUseCase @Inject constructor(
    private val repository: ReviewsRepository,
    private val mutationNotifier: ReviewMutationNotifier,
) {
    suspend operator fun invoke(reviewId: Int, vote: ReviewVote) =
        repository.vote(reviewId, vote).also { mutationNotifier.notifyChanged() }
}

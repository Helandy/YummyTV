package su.afk.yummy.tv.domain.reviews.usecase

import su.afk.yummy.tv.domain.reviews.ReviewMutationNotifier
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import javax.inject.Inject

/** Удаляет выбранную рецензию и уведомляет наблюдателей об изменении списка. */
class DeleteReviewUseCase @Inject constructor(
    private val repository: ReviewsRepository,
    private val mutationNotifier: ReviewMutationNotifier,
) {
    suspend operator fun invoke(reviewId: Int) = repository.delete(reviewId).also { deleted ->
        if (deleted) mutationNotifier.notifyChanged()
    }
}

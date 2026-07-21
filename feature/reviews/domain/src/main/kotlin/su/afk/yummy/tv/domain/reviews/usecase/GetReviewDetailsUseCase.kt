package su.afk.yummy.tv.domain.reviews.usecase

import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import javax.inject.Inject

/** Загружает подробные данные выбранной рецензии. */
class GetReviewDetailsUseCase @Inject constructor(private val repository: ReviewsRepository) {
    suspend operator fun invoke(reviewId: Int) = repository.getReview(reviewId)
}

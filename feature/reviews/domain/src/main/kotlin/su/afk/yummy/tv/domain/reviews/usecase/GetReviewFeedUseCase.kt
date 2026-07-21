package su.afk.yummy.tv.domain.reviews.usecase

import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import javax.inject.Inject

/** Загружает страницу общей ленты рецензий с выбранной сортировкой. */
class GetReviewFeedUseCase @Inject constructor(private val repository: ReviewsRepository) {
    suspend operator fun invoke(sort: ReviewSort, limit: Int, offset: Int) =
        repository.getReviews(sort, limit, offset)
}

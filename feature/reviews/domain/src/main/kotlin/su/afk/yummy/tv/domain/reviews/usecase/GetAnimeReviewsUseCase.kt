package su.afk.yummy.tv.domain.reviews.usecase

import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import javax.inject.Inject

/** Загружает страницу рецензий выбранного аниме с указанной сортировкой. */
class GetAnimeReviewsUseCase @Inject constructor(private val repository: ReviewsRepository) {
    suspend operator fun invoke(animeId: Int, sort: ReviewSort, limit: Int, offset: Int) =
        repository.getAnimeReviews(animeId, sort, limit, offset)
}

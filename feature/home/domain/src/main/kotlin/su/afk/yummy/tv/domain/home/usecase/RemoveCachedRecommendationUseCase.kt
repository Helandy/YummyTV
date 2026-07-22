package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Убирает тайтл из кешированного блока рекомендаций главной. */
class RemoveCachedRecommendationUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(animeId: Int) {
        homeFeedRepository.removeCachedRecommendation(animeId)
    }
}

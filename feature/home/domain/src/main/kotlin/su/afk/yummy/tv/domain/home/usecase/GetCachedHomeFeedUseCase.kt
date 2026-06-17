package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Читает кешированную главную ленту без сетевого обновления. */
class GetCachedHomeFeedUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(): HomeFeed? = homeFeedRepository.getCachedHomeFeed()
}

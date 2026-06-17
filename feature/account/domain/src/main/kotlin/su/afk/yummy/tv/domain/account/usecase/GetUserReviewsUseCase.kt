package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository
import javax.inject.Inject

class GetUserReviewsUseCase @Inject constructor(
    private val repository: UserProfileContentRepository,
) {
    suspend operator fun invoke(userId: Int, limit: Int, offset: Int) =
        repository.getReviews(userId, limit, offset)
}

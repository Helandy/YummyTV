package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserProfileRepository
import javax.inject.Inject

/** Loads the site-style profile summary for a Yani user. */
class GetUserProfileSummaryUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(userId: Int) = repository.getUserProfileSummary(userId)
}

package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserProfileRepository
import javax.inject.Inject

/** Загружает краткую сводку профиля пользователя Yani. */
class GetUserProfileSummaryUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(userId: Int) = repository.getUserProfileSummary(userId)
}

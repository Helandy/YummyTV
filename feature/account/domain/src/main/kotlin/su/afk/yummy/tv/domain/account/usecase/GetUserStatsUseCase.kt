package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserStatsRepository

class GetUserStatsUseCase(private val repository: UserStatsRepository) {
    suspend operator fun invoke(userId: Int) = repository.getUserStats(userId)
}

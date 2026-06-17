package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserStatsRepository
import javax.inject.Inject

/** Загружает статистику профиля пользователя Yani. */
class GetUserStatsUseCase @Inject constructor(private val repository: UserStatsRepository) {
    suspend operator fun invoke(userId: Int) = repository.getUserStats(userId)
}

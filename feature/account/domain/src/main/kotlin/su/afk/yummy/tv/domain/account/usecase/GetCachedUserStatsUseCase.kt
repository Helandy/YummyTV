package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.repository.UserStatsRepository
import javax.inject.Inject

/** Возвращает статистику профиля пользователя Yani из локального кэша без обращения к сети и проверки свежести. */
class GetCachedUserStatsUseCase @Inject constructor(private val repository: UserStatsRepository) {
    suspend operator fun invoke(userId: Int) = repository.getCachedUserStats(userId)
}

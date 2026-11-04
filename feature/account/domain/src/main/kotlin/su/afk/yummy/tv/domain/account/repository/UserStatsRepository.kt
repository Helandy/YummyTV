package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.UserStats

interface UserStatsRepository {
    suspend fun getUserStats(userId: Int): UserStats

    /** Статистика из локального кэша независимо от его возраста; `null`, если её там нет. */
    suspend fun getCachedUserStats(userId: Int): UserStats?
}

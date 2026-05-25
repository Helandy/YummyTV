package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.UserStats

interface UserStatsRepository {
    suspend fun getUserStats(userId: Int): UserStats
}

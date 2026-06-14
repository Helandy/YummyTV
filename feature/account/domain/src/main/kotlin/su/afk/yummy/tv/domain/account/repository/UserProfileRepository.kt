package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.UserProfileSummary

interface UserProfileRepository {
    suspend fun getUserProfileSummary(userId: Int): UserProfileSummary
}

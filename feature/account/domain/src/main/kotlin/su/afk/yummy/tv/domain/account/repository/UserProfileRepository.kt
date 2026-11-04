package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.UserProfileSummary

interface UserProfileRepository {
    suspend fun getUserProfileSummary(userId: Int): UserProfileSummary

    /** Сводка из локального кэша независимо от его возраста; `null`, если её там нет. */
    suspend fun getCachedUserProfileSummary(userId: Int): UserProfileSummary?
}

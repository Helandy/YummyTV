package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toUserProfileSummary
import su.afk.yummy.tv.data.account.storage.mapper.toUserProfileSummaryCache
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.repository.UserProfileRepository

class YaniUserProfileRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : UserProfileRepository {
    override suspend fun getUserProfileSummary(userId: Int): UserProfileSummary =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = accountStorage.getUserProfileSummary(userId, languageCode)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toUserProfileSummary()
            }

            try {
                fetchUserProfileSummary(userId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toUserProfileSummary()
                    ?: throw error
            }
        }

    private suspend fun fetchUserProfileSummary(
        userId: Int,
        languageCode: String,
    ): UserProfileSummary {
        val cache = api.getUserProfile(userId).response.toUserProfileSummaryCache(
            userId = userId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveUserProfileSummary(cache)
        return cache.toUserProfileSummary()
    }
}

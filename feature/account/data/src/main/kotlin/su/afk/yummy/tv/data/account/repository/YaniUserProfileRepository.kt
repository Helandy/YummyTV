package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toUserProfileSummary
import su.afk.yummy.tv.data.account.storage.mapper.toUserProfileSummaryCache
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.repository.UserProfileRepository

class YaniUserProfileRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : UserProfileRepository {
    override suspend fun getUserProfileSummary(userId: Int): UserProfileSummary =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { accountStorage.getUserProfileSummary(userId, languageCode) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toUserProfileSummary() },
                fetchAndSave = {
                    val cache = api.getUserProfile(userId).response.toUserProfileSummaryCache(
                        userId = userId,
                        language = languageCode,
                        cachedAt = System.currentTimeMillis(),
                    )
                    accountStorage.saveUserProfileSummary(cache)
                    cache
                },
            )
        }
}

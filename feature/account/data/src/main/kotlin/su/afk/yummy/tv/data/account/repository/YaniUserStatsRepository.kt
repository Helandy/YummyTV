package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.YaniUserStatsDtoBundle
import su.afk.yummy.tv.data.account.storage.mapper.toUserStatsCache
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.repository.UserStatsRepository
import su.afk.yummy.tv.data.account.storage.mapper.toUserStats as toStoredUserStats

class YaniUserStatsRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : UserStatsRepository {
    override suspend fun getUserStats(userId: Int): UserStats = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.currentLanguageCode()
        offlineFirstCache(
            read = { accountStorage.getUserStats(userId, languageCode) },
            isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
            toDomain = { it.toStoredUserStats() },
            fetchAndSave = { fetchUserStats(userId, languageCode) },
        )
    }

    private suspend fun fetchUserStats(userId: Int, languageCode: String) =
        coroutineScope {
            val genres = async { api.getUserStatsGenres(userId) }
            val ratings = async { api.getUserStatsRatings(userId) }
            val lists = async { api.getUserStatsLists(userId) }
            val types = async { api.getUserStatsTypes(userId) }

            val cache = YaniUserStatsDtoBundle(
                genres = genres.await(),
                ratings = ratings.await(),
                lists = lists.await(),
                types = types.await(),
            ).toUserStatsCache(
                userId = userId,
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
            accountStorage.saveUserStats(cache)
            cache
        }
}

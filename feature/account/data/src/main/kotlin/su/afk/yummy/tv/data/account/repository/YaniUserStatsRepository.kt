package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeTypeStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserGenreStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserListWatchStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserRatingStatDto
import su.afk.yummy.tv.data.account.mapper.toAnimeTypeStat
import su.afk.yummy.tv.data.account.mapper.toGenreStat
import su.afk.yummy.tv.data.account.mapper.toListWatchStat
import su.afk.yummy.tv.data.account.mapper.toRatingStat
import su.afk.yummy.tv.data.account.mapper.toUserStats
import su.afk.yummy.tv.data.account.mapper.toUserStatsCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.repository.UserStatsRepository

class YaniUserStatsRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val accountStorage: AccountStorageStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : UserStatsRepository {
    override suspend fun getUserStats(userId: Int): UserStats = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = accountStorage.getUserStats(userId, languageCode)
        if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
            return@withContext stored.toUserStats()
        }

        try {
            fetchUserStats(userId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toUserStats()
                ?: readLegacyUserStats(userId, language, languageCode)
                ?: throw error
        }
    }

    private suspend fun fetchUserStats(userId: Int, languageCode: String): UserStats =
        coroutineScope {
            val genres = async { api.getUserStatsGenres(userId) }
            val ratings = async { api.getUserStatsRatings(userId) }
            val lists = async { api.getUserStatsLists(userId) }
            val types = async { api.getUserStatsTypes(userId) }

            val stats = YaniUserStatsCacheDto(
                genres = genres.await(),
                ratings = ratings.await(),
                lists = lists.await(),
                types = types.await(),
            ).toUserStats()

            accountStorage.saveUserStats(
                stats.toUserStatsCache(
                    userId = userId,
                    language = languageCode,
                    cachedAt = System.currentTimeMillis(),
                )
        )
            stats
        }

    private suspend fun readLegacyUserStats(
        userId: Int,
        language: YaniContentLanguage,
        languageCode: String,
    ): UserStats? {
        val cached = cache.getCached<YaniUserStatsCacheDto>(
            key = YaniAccountCacheKeys.userStats(userId, language),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val stats = cached.value.toUserStats()
        accountStorage.saveUserStats(
            stats.toUserStatsCache(
                userId = userId,
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return stats
    }
}

private fun YaniUserStatsCacheDto.toUserStats(): UserStats =
    UserStats(
        genres = genres.map { it.toGenreStat() },
        ratings = ratings.map { it.toRatingStat() },
        lists = lists.mapNotNull { it.toListWatchStat() },
        types = types.mapNotNull { it.toAnimeTypeStat() },
    )

@Serializable
private data class YaniUserStatsCacheDto(
    val genres: List<YaniUserGenreStatDto> = emptyList(),
    val ratings: List<YaniUserRatingStatDto> = emptyList(),
    val lists: List<YaniUserListWatchStatDto> = emptyList(),
    val types: List<YaniUserAnimeTypeStatDto> = emptyList(),
)

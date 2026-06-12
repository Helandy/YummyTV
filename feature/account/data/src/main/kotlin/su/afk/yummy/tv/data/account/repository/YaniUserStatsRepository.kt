package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeTypeStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserGenreStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserListWatchStatDto
import su.afk.yummy.tv.data.account.dto.YaniUserRatingStatDto
import su.afk.yummy.tv.data.account.mapper.toAnimeTypeStat
import su.afk.yummy.tv.data.account.mapper.toGenreStat
import su.afk.yummy.tv.data.account.mapper.toListWatchStat
import su.afk.yummy.tv.data.account.mapper.toRatingStat
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.repository.UserStatsRepository

class YaniUserStatsRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : UserStatsRepository {
    override suspend fun getUserStats(userId: Int): UserStats = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val response = cache.getOrFetch(
            key = YaniAccountCacheKeys.userStats(userId, language),
            ttlMs = ACCOUNT_MEDIUM_TTL_MS,
            serialize = { dto: YaniUserStatsCacheDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = {
                val genres = async { api.getUserStatsGenres(userId) }
                val ratings = async { api.getUserStatsRatings(userId) }
                val lists = async { api.getUserStatsLists(userId) }
                val types = async { api.getUserStatsTypes(userId) }

                YaniUserStatsCacheDto(
                    genres = genres.await(),
                    ratings = ratings.await(),
                    lists = lists.await(),
                    types = types.await(),
                )
            },
        )

        UserStats(
            genres = response.genres.map { it.toGenreStat() },
            ratings = response.ratings.map { it.toRatingStat() },
            lists = response.lists.mapNotNull { it.toListWatchStat() },
            types = response.types.mapNotNull { it.toAnimeTypeStat() },
        )
    }
}

@Serializable
private data class YaniUserStatsCacheDto(
    val genres: List<YaniUserGenreStatDto> = emptyList(),
    val ratings: List<YaniUserRatingStatDto> = emptyList(),
    val lists: List<YaniUserListWatchStatDto> = emptyList(),
    val types: List<YaniUserAnimeTypeStatDto> = emptyList(),
)

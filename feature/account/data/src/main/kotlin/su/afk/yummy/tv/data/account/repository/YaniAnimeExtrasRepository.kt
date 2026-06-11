package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingResponseDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniListStatsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniRatingResponseDto
import su.afk.yummy.tv.data.account.mapper.toCollectionSummary
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

class YaniAnimeExtrasRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : AnimeExtrasRepository {

    override suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary =
        withContext(Dispatchers.IO) {
            val buckets = cache.getOrFetch(
                key = YaniAccountCacheKeys.ratingBuckets(animeId),
                ttlMs = ACCOUNT_MEDIUM_TTL_MS,
                serialize = { dto: YaniRatingResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { YaniRatingResponseDto(response = api.getRatingBuckets(animeId)) },
            ).response
            AnimeRatingSummary(
                distribution = buckets.map { AnimeRatingBucket(it.rating, it.count) },
                userRating = null,
            )
        }

    override suspend fun getUserRating(animeId: Int): Int? =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            cache.getOrFetch(
                key = YaniAccountCacheKeys.userRating(userId, animeId),
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniAnimeUserRatingResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { YaniAnimeUserRatingResponseDto(response = api.getUserRating(animeId)) },
            ).response
                .user
                ?.rating
                ?.toInt()
                ?.takeIf { it in 1..10 }
        }

    override suspend fun setRating(animeId: Int, rating: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.setRating(animeId, rating)
        updateCachedUserRating(userId, animeId, rating)
        cache.invalidate(YaniAccountCacheKeys.ratingBuckets(animeId))
    }

    override suspend fun deleteRating(animeId: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.deleteRating(animeId)
        updateCachedUserRating(userId, animeId, rating = null)
        cache.invalidate(YaniAccountCacheKeys.ratingBuckets(animeId))
    }

    override suspend fun getListStats(animeId: Int): AnimeListStats =
        withContext(Dispatchers.IO) {
            val stats = cache.getOrFetch(
                key = YaniAccountCacheKeys.listStats(animeId),
                ttlMs = ACCOUNT_MEDIUM_TTL_MS,
                serialize = { dto: YaniListStatsResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { YaniListStatsResponseDto(response = api.getListStats(animeId)) },
            ).response
            AnimeListStats(stats.associate { it.listId to it.count })
        }

    override suspend fun getCollections(animeId: Int, limit: Int, offset: Int): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            cache.getOrFetch(
                key = YaniAccountCacheKeys.animeCollections(animeId, limit, offset),
                ttlMs = ACCOUNT_MEDIUM_TTL_MS,
                serialize = { dto: YaniCollectionsResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = {
                    YaniCollectionsResponseDto(
                        response = api.getAnimeCollections(
                            animeId,
                            limit,
                            offset
                        )
                    )
                },
            ).response.mapNotNull { it.toCollectionSummary() }
        }

    override suspend fun getCollections(limit: Int, offset: Int): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            cache.getOrFetch(
                key = YaniAccountCacheKeys.collections(limit, offset),
                ttlMs = ACCOUNT_MEDIUM_TTL_MS,
                serialize = { dto: YaniCollectionsResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = {
                    YaniCollectionsResponseDto(
                        response = api.getCollections(
                            limit,
                            offset
                        )
                    )
                },
            ).response.mapNotNull { it.toCollectionSummary() }
        }

    private suspend fun currentUserId(): Int =
        settingsStore.yaniUserId.first()

    private suspend fun updateCachedUserRating(userId: Int, animeId: Int, rating: Int?) {
        cache.put(
            key = YaniAccountCacheKeys.userRating(userId, animeId),
            serialize = { dto: YaniAnimeUserRatingResponseDto -> json.encodeToString(dto) },
            value = YaniAnimeUserRatingResponseDto(
                response = YaniAnimeUserRatingDto(
                    user = rating?.let { YaniAnimeUserDto(rating = it.toDouble()) },
                ),
            ),
        )
    }
}

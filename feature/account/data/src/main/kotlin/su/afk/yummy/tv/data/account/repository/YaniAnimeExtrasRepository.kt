package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.AccountUserRatingEntry
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingResponseDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniListStatsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniRatingResponseDto
import su.afk.yummy.tv.data.account.mapper.toAnimeListStats
import su.afk.yummy.tv.data.account.mapper.toCollectionSummaries
import su.afk.yummy.tv.data.account.mapper.toCollectionSummary
import su.afk.yummy.tv.data.account.mapper.toCollectionsPageCache
import su.afk.yummy.tv.data.account.mapper.toListStatsCache
import su.afk.yummy.tv.data.account.mapper.toRatingBucketsCache
import su.afk.yummy.tv.data.account.mapper.toRatingSummary
import su.afk.yummy.tv.data.account.mapper.toUserRating
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

class YaniAnimeExtrasRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val accountStorage: AccountStorageStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : AnimeExtrasRepository {

    override suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary =
        withContext(Dispatchers.IO) {
            val stored = accountStorage.getRatingBuckets(animeId)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toRatingSummary()
            }

            try {
                fetchRatingSummary(animeId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toRatingSummary()
                    ?: readLegacyRatingSummary(animeId)
                    ?: throw error
            }
        }

    override suspend fun getUserRating(animeId: Int): Int? =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            val stored = accountStorage.getUserRating(userId, animeId)
            if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
                return@withContext stored.toUserRating()
            }

            try {
                fetchUserRating(userId, animeId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (stored != null) {
                    stored.toUserRating()
                } else {
                    readLegacyUserRating(userId, animeId)?.toUserRating() ?: throw error
                }
            }
        }

    override suspend fun setRating(animeId: Int, rating: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.setRating(animeId, rating)
        updateCachedUserRating(userId, animeId, rating)
        accountStorage.deleteRatingBuckets(animeId)
        cache.invalidate(YaniAccountCacheKeys.ratingBuckets(animeId))
        cache.invalidate(YaniAccountCacheKeys.userRating(userId, animeId))
    }

    override suspend fun deleteRating(animeId: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.deleteRating(animeId)
        updateCachedUserRating(userId, animeId, rating = null)
        accountStorage.deleteRatingBuckets(animeId)
        cache.invalidate(YaniAccountCacheKeys.ratingBuckets(animeId))
        cache.invalidate(YaniAccountCacheKeys.userRating(userId, animeId))
    }

    override suspend fun getListStats(animeId: Int): AnimeListStats =
        withContext(Dispatchers.IO) {
            val stored = accountStorage.getListStats(animeId)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toAnimeListStats()
            }

            try {
                fetchListStats(animeId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toAnimeListStats()
                    ?: readLegacyListStats(animeId)
                    ?: throw error
            }
        }

    override suspend fun getCollections(animeId: Int, limit: Int, offset: Int): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val pageKey = animeCollectionsPageKey(animeId, limit, offset, languageCode)
            getCollectionsPage(
                pageKey = pageKey,
                languageCode = languageCode,
                fetch = {
                    api.getAnimeCollections(animeId, limit, offset)
                        .mapNotNull { it.toCollectionSummary() }
                },
                legacyKey = {
                    YaniAccountCacheKeys.animeCollections(
                        animeId,
                        limit,
                        offset,
                        language
                    )
                },
            )
        }

    override suspend fun getCollections(limit: Int, offset: Int): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val pageKey = collectionsPageKey(limit, offset, languageCode)
            getCollectionsPage(
                pageKey = pageKey,
                languageCode = languageCode,
                fetch = {
                    api.getCollections(limit, offset).mapNotNull { it.toCollectionSummary() }
                },
                legacyKey = { YaniAccountCacheKeys.collections(limit, offset, language) },
            )
        }

    private suspend fun currentUserId(): Int =
        settingsStore.yaniUserId.first()

    private suspend fun fetchRatingSummary(animeId: Int): AnimeRatingSummary {
        val buckets = api.getRatingBuckets(animeId).map { AnimeRatingBucket(it.rating, it.count) }
        accountStorage.saveRatingBuckets(
            buckets.toRatingBucketsCache(
                animeId = animeId,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return AnimeRatingSummary(distribution = buckets, userRating = null)
    }

    private suspend fun readLegacyRatingSummary(animeId: Int): AnimeRatingSummary? {
        val cached = cache.getCached<YaniRatingResponseDto>(
            key = YaniAccountCacheKeys.ratingBuckets(animeId),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val buckets = cached.value.response.map { AnimeRatingBucket(it.rating, it.count) }
        accountStorage.saveRatingBuckets(
            buckets.toRatingBucketsCache(
                animeId = animeId,
                cachedAt = cached.cachedAt,
            )
        )
        return AnimeRatingSummary(distribution = buckets, userRating = null)
    }

    private suspend fun fetchUserRating(userId: Int, animeId: Int): Int? {
        val rating = api.getUserRating(animeId)
            .user
            ?.rating
            ?.toInt()
            ?.takeIf { it in 1..10 }
        accountStorage.saveUserRating(
            AccountUserRatingEntry(
                userId = userId,
                animeId = animeId,
                rating = rating,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return rating
    }

    private suspend fun readLegacyUserRating(userId: Int, animeId: Int): AccountUserRatingEntry? {
        val cached = cache.getCached<YaniAnimeUserRatingResponseDto>(
            key = YaniAccountCacheKeys.userRating(userId, animeId),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val rating = cached.value.response.user?.rating?.toInt()?.takeIf { it in 1..10 }
        return AccountUserRatingEntry(
            userId = userId,
            animeId = animeId,
            rating = rating,
            cachedAt = cached.cachedAt,
        ).also {
            accountStorage.saveUserRating(it)
        }
    }

    private suspend fun fetchListStats(animeId: Int): AnimeListStats {
        val stats = AnimeListStats(api.getListStats(animeId).associate { it.listId to it.count })
        accountStorage.saveListStats(stats.toListStatsCache(animeId, System.currentTimeMillis()))
        return stats
    }

    private suspend fun readLegacyListStats(animeId: Int): AnimeListStats? {
        val cached = cache.getCached<YaniListStatsResponseDto>(
            key = YaniAccountCacheKeys.listStats(animeId),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val stats = AnimeListStats(cached.value.response.associate { it.listId to it.count })
        accountStorage.saveListStats(stats.toListStatsCache(animeId, cached.cachedAt))
        return stats
    }

    private suspend fun getCollectionsPage(
        pageKey: String,
        languageCode: String,
        fetch: suspend () -> List<AnimeCollectionSummary>,
        legacyKey: () -> String,
    ): List<AnimeCollectionSummary> {
        val stored = accountStorage.getCollections(pageKey)
        if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
            return stored.toCollectionSummaries()
        }

        return try {
            fetch().also { collections ->
                accountStorage.saveCollections(
                    collections.toCollectionsPageCache(
                        pageKey = pageKey,
                        language = languageCode,
                        cachedAt = System.currentTimeMillis(),
                    )
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toCollectionSummaries()
                ?: readLegacyCollections(pageKey, languageCode, legacyKey())
                ?: throw error
        }
    }

    private suspend fun readLegacyCollections(
        pageKey: String,
        languageCode: String,
        key: String,
    ): List<AnimeCollectionSummary>? {
        val cached = cache.getCached<YaniCollectionsResponseDto>(
            key = key,
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val collections = cached.value.response.mapNotNull { it.toCollectionSummary() }
        accountStorage.saveCollections(
            collections.toCollectionsPageCache(
                pageKey = pageKey,
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return collections
    }

    private suspend fun updateCachedUserRating(userId: Int, animeId: Int, rating: Int?) {
        accountStorage.saveUserRating(
            AccountUserRatingEntry(
                userId = userId,
                animeId = animeId,
                rating = rating,
                cachedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun animeCollectionsPageKey(
        animeId: Int,
        limit: Int,
        offset: Int,
        language: String
    ): String =
        "anime:$animeId:$limit:$offset:$language"

    private fun collectionsPageKey(limit: Int, offset: Int, language: String): String =
        "all:$limit:$offset:$language"
}

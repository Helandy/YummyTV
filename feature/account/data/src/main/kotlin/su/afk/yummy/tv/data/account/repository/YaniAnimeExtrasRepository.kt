package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.data.account.dto.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toAnimeListStats
import su.afk.yummy.tv.data.account.storage.mapper.toCollectionSummaries
import su.afk.yummy.tv.data.account.storage.mapper.toCollectionsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toListStatsCache
import su.afk.yummy.tv.data.account.storage.mapper.toRatingBucketsCache
import su.afk.yummy.tv.data.account.storage.mapper.toRatingSummary
import su.afk.yummy.tv.data.account.storage.mapper.toUserRating
import su.afk.yummy.tv.data.account.storage.mapper.toUserRatingEntry
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

class YaniAnimeExtrasRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorageStore,
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
                    throw error
                }
            }
        }

    override suspend fun setRating(animeId: Int, rating: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.setRating(animeId, rating)
        updateCachedUserRating(userId, animeId, rating)
        accountStorage.deleteRatingBuckets(animeId)
    }

    override suspend fun deleteRating(animeId: Int) = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        api.deleteRating(animeId)
        updateCachedUserRating(userId, animeId, rating = null)
        accountStorage.deleteRatingBuckets(animeId)
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
                    ?: throw error
            }
        }

    override suspend fun getCachedListStats(animeId: Int): AnimeListStats? =
        withContext(Dispatchers.IO) {
            accountStorage.getListStats(animeId)
                ?.takeIf { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) }
                ?.toAnimeListStats()
        }

    override suspend fun getCollections(
        animeId: Int,
        limit: Int,
        offset: Int
    ): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val pageKey = animeCollectionsPageKey(animeId, limit, offset, languageCode)
            getCollectionsPage(
                pageKey = pageKey,
                languageCode = languageCode,
                fetch = {
                    api.getAnimeCollections(animeId, limit, offset)
                },
            )
        }

    private suspend fun currentUserId(): Int =
        settingsStore.yaniUserId.first()

    private suspend fun fetchRatingSummary(animeId: Int): AnimeRatingSummary {
        val cache = api.getRatingBuckets(animeId).toRatingBucketsCache(
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveRatingBuckets(cache)
        return cache.toRatingSummary()
    }

    private suspend fun fetchListStats(animeId: Int): AnimeListStats {
        val cache = api.getAnimeListStats(animeId).toListStatsCache(
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveListStats(cache)
        return cache.toAnimeListStats()
    }

    private suspend fun fetchUserRating(userId: Int, animeId: Int): Int? {
        val rating = api.getUserRating(animeId)
            .user
            ?.rating
            ?.toInt()
            ?.takeIf { it in 1..10 }
        val entry = rating.toUserRatingEntry(
            userId = userId,
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveUserRating(entry)
        return entry.toUserRating()
    }

    private suspend fun getCollectionsPage(
        pageKey: String,
        languageCode: String,
        fetch: suspend () -> List<YaniCollectionSummaryDto>,
    ): List<AnimeCollectionSummary> {
        val stored = accountStorage.getCollections(pageKey)
        if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
            return stored.toCollectionSummaries()
        }

        return try {
            val cachedAt = System.currentTimeMillis()
            val cache = fetch().toCollectionsPageCache(
                pageKey = pageKey,
                language = languageCode,
                cachedAt = cachedAt,
            )
            accountStorage.saveCollections(
                cache,
                prunePagesCachedBefore = cachedAt - ACCOUNT_PAGE_CACHE_RETENTION_MS,
            )
            cache.toCollectionSummaries()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toCollectionSummaries()
                ?: throw error
        }
    }

    private suspend fun updateCachedUserRating(userId: Int, animeId: Int, rating: Int?) {
        accountStorage.saveUserRating(
            rating.toUserRatingEntry(
                userId = userId,
                animeId = animeId,
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
}

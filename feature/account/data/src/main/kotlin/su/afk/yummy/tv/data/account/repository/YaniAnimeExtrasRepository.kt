package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.preferences.settings.currentUserId
import su.afk.yummy.tv.core.storage.account.AccountListStatsCache
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketsCache
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.AccountUserRatingEntry
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
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
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : AnimeExtrasRepository {

    override suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary =
        withContext(Dispatchers.IO) {
            offlineFirstCache(
                read = { accountStorage.getRatingBuckets(animeId) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toRatingSummary() },
                fetchAndSave = { fetchRatingSummary(animeId) },
            )
        }

    override suspend fun getUserRating(animeId: Int): Int? =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            offlineFirstCache(
                read = { accountStorage.getUserRating(userId, animeId) },
                isFresh = { it.isFresh(ACCOUNT_SHORT_TTL_MS) },
                toDomain = { it.toUserRating() },
                fetchAndSave = { fetchUserRating(userId, animeId) },
            )
        }

    override suspend fun setRating(animeId: Int, rating: Int) = withContext(Dispatchers.IO) {
        val userId = settingsStore.currentUserId()
        api.setRating(animeId, rating)
        updateCachedUserRating(userId, animeId, rating)
        accountStorage.deleteRatingBuckets(animeId)
    }

    override suspend fun deleteRating(animeId: Int) = withContext(Dispatchers.IO) {
        val userId = settingsStore.currentUserId()
        api.deleteRating(animeId)
        updateCachedUserRating(userId, animeId, rating = null)
        accountStorage.deleteRatingBuckets(animeId)
    }

    override suspend fun getListStats(animeId: Int): AnimeListStats =
        withContext(Dispatchers.IO) {
            offlineFirstCache(
                read = { accountStorage.getListStats(animeId) },
                isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
                toDomain = { it.toAnimeListStats() },
                fetchAndSave = { fetchListStats(animeId) },
            )
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
            val languageCode = settingsStore.currentLanguageCode()
            val pageKey = animeCollectionsPageKey(animeId, limit, offset, languageCode)
            getCollectionsPage(
                pageKey = pageKey,
                languageCode = languageCode,
                fetch = {
                    api.getAnimeCollections(animeId, limit, offset)
                },
            )
        }

    private suspend fun fetchRatingSummary(animeId: Int): AccountRatingBucketsCache {
        val cache = api.getRatingBuckets(animeId).toRatingBucketsCache(
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveRatingBuckets(cache)
        return cache
    }

    private suspend fun fetchListStats(animeId: Int): AccountListStatsCache {
        val cache = api.getAnimeListStats(animeId).toListStatsCache(
            animeId = animeId,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveListStats(cache)
        return cache
    }

    private suspend fun fetchUserRating(userId: Int, animeId: Int): AccountUserRatingEntry {
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
        return entry
    }

    private suspend fun getCollectionsPage(
        pageKey: String,
        languageCode: String,
        fetch: suspend () -> List<YaniCollectionSummaryDto>,
    ): List<AnimeCollectionSummary> = offlineFirstCache(
        read = { accountStorage.getCollections(pageKey) },
        isFresh = { it.isFresh(ACCOUNT_MEDIUM_TTL_MS) },
        toDomain = { it.toCollectionSummaries() },
        fetchAndSave = {
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
            cache
        },
    )

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

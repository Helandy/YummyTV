package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary

interface AnimeExtrasRepository {
    suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary
    suspend fun getUserRating(animeId: Int): Int?
    suspend fun setRating(animeId: Int, rating: Int)
    suspend fun deleteRating(animeId: Int)
    suspend fun getListStats(animeId: Int): AnimeListStats
    suspend fun getCachedListStats(animeId: Int): AnimeListStats?
    suspend fun getCollections(animeId: Int, limit: Int = 20, offset: Int = 0): List<AnimeCollectionSummary>
}

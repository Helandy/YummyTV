package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.mapper.toCollectionSummary
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

class YaniAnimeExtrasRepository(
    private val api: YaniAccountApi,
) : AnimeExtrasRepository {

    override suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary =
        withContext(Dispatchers.IO) { AnimeRatingSummary(
            distribution = api.getRatingBuckets(animeId).map { AnimeRatingBucket(it.rating, it.count) },
            userRating = null,
        ) }

    override suspend fun getUserRating(animeId: Int): Int? =
        withContext(Dispatchers.IO) {
            api.getUserRating(animeId)
                .user
                ?.rating
                ?.toInt()
                ?.takeIf { it in 1..10 }
        }

    override suspend fun setRating(animeId: Int, rating: Int) = withContext(Dispatchers.IO) {
        api.setRating(animeId, rating)
    }

    override suspend fun deleteRating(animeId: Int) = withContext(Dispatchers.IO) {
        api.deleteRating(animeId)
    }

    override suspend fun getListStats(animeId: Int): AnimeListStats =
        withContext(Dispatchers.IO) { AnimeListStats(api.getListStats(animeId).associate { it.listId to it.count }) }

    override suspend fun getCollections(animeId: Int, limit: Int, offset: Int): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) { api.getAnimeCollections(animeId, limit, offset).mapNotNull { it.toCollectionSummary() } }

    override suspend fun getCollections(limit: Int, offset: Int): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) { api.getCollections(limit, offset).mapNotNull { it.toCollectionSummary() } }
}

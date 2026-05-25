package su.afk.yummy.tv.data.account.repository

import su.afk.yummy.tv.data.account.mapper.toCollectionSummary
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.AnimeListStats
import su.afk.yummy.tv.domain.account.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.AnimeRatingSummary

class YaniAnimeExtrasRepository(
    private val api: YaniAccountApi,
) : AnimeExtrasRepository {

    override suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary =
        AnimeRatingSummary(
            distribution = api.getRatingBuckets(animeId).map { AnimeRatingBucket(it.rating, it.count) },
            userRating = null,
        )

    override suspend fun getUserRating(animeId: Int): Int? =
        api.getUserRating(animeId)
            .user
            ?.rating
            ?.toInt()
            ?.takeIf { it in 1..10 }

    override suspend fun setRating(animeId: Int, rating: Int) {
        api.setRating(animeId, rating)
    }

    override suspend fun deleteRating(animeId: Int) {
        api.deleteRating(animeId)
    }

    override suspend fun getListStats(animeId: Int): AnimeListStats =
        AnimeListStats(api.getListStats(animeId).associate { it.listId to it.count })

    override suspend fun getCollections(animeId: Int, limit: Int, offset: Int): List<AnimeCollectionSummary> =
        api.getAnimeCollections(animeId, limit, offset).mapNotNull { it.toCollectionSummary() }

    override suspend fun getCollections(limit: Int, offset: Int): List<AnimeCollectionSummary> =
        api.getCollections(limit, offset).mapNotNull { it.toCollectionSummary() }
}

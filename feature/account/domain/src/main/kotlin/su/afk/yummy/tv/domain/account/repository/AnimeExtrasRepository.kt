package su.afk.yummy.tv.domain.account

interface AnimeExtrasRepository {
    suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary
    suspend fun getUserRating(animeId: Int): Int?
    suspend fun setRating(animeId: Int, rating: Int)
    suspend fun deleteRating(animeId: Int)
    suspend fun getListStats(animeId: Int): AnimeListStats
    suspend fun getCollections(animeId: Int, limit: Int = 20, offset: Int = 0): List<AnimeCollectionSummary>
    suspend fun getCollections(limit: Int = 40, offset: Int = 0): List<AnimeCollectionSummary>
}

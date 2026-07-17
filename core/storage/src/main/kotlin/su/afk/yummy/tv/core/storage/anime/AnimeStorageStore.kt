package su.afk.yummy.tv.core.storage.anime

class AnimeStorageStore(private val dao: AnimeStorageDao) {

    suspend fun getDetails(animeId: Int, language: String): AnimeDetailsCache? =
        dao.getDetails(animeId, language)

    suspend fun saveDetails(cache: AnimeDetailsCache) {
        dao.replaceDetails(cache)
    }

    suspend fun deleteDetails(animeId: Int, language: String) {
        dao.deleteDetails(animeId, language)
    }

    suspend fun expireAllDetails() {
        dao.expireAllDetails()
    }

    suspend fun getVideos(animeId: Int, language: String): AnimeVideosCache? =
        dao.getVideos(animeId, language)

    suspend fun saveVideos(cache: AnimeVideosCache) {
        dao.replaceVideos(cache)
    }

    suspend fun getRecommendations(
        animeId: Int,
        language: String,
        fromAi: Boolean,
    ): AnimeRecommendationsCache? =
        dao.getRecommendations(animeId, language, fromAi)

    suspend fun saveRecommendations(cache: AnimeRecommendationsCache) {
        dao.replaceRecommendations(cache)
    }

    suspend fun getTrailers(animeId: Int, language: String): AnimeTrailersCache? =
        dao.getTrailers(animeId, language)

    suspend fun saveTrailers(cache: AnimeTrailersCache) {
        dao.replaceTrailers(cache)
    }
}

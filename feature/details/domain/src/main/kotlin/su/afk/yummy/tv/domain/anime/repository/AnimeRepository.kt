package su.afk.yummy.tv.domain.anime

interface AnimeRepository {
    suspend fun getAnimeDetails(animeId: Int): AnimeDetails
    suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo>
    suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer>
    suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): List<AnimeRecommendation>
}

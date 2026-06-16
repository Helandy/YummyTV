package su.afk.yummy.tv.domain.anime.repository

import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo

interface AnimeRepository {
    suspend fun getAnimeDetails(animeId: Int): AnimeDetails
    suspend fun getCachedAnimeDetails(animeId: Int): AnimeDetails?
    suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo>
    suspend fun getCachedAnimeVideos(animeId: Int): List<AnimeVideo>?
    suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer>
    suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): List<AnimeRecommendation>
}

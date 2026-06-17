package su.afk.yummy.tv.domain.anime.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.model.AnimeWatchProgress

interface AnimeRepository {
    suspend fun getAnimeDetails(animeId: Int): AnimeDetails
    suspend fun getCachedAnimeDetails(animeId: Int): AnimeDetails?
    suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo>
    suspend fun refreshAnimeVideos(animeId: Int): List<AnimeVideo>
    suspend fun getCachedAnimeVideos(animeId: Int): List<AnimeVideo>?
    suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer>
    suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): List<AnimeRecommendation>
    fun observeWatchProgress(animeId: Int): Flow<List<AnimeWatchProgress>>
}

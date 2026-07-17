package su.afk.yummy.tv.domain.anime.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress

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

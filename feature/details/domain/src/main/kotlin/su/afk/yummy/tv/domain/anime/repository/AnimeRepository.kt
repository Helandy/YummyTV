package su.afk.yummy.tv.domain.anime.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationReaction
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationReference

interface AnimeRepository {
    suspend fun getAnimeDetails(animeId: Int): AnimeDetails
    suspend fun getCachedAnimeDetails(animeId: Int): AnimeDetails?
    suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo>
    suspend fun refreshAnimeVideos(animeId: Int): List<AnimeVideo>
    suspend fun getCachedAnimeVideos(animeId: Int): List<AnimeVideo>?
    suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer>
    suspend fun getAnimeRelation(reference: AnimeRelationReference): AnimeRelation
    suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): List<AnimeRecommendation>
    suspend fun setAnimeRecommendationIgnored(animeId: Int, ignored: Boolean): Boolean
    suspend fun voteAnimeRecommendation(
        animeId: Int,
        similarAnimeId: Int,
        vote: AnimeRecommendationVote,
    ): AnimeRecommendationReaction

    fun observeWatchProgress(animeId: Int): Flow<List<AnimeWatchProgress>>
}

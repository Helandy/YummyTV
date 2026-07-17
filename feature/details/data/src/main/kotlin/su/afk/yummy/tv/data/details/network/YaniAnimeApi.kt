package su.afk.yummy.tv.data.details.network

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideosDto
import su.afk.yummy.tv.data.details.dto.YaniDirectorDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniGenreDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationMutationResponseDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationVoteBodyDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationVoteResponseDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationsDto
import su.afk.yummy.tv.data.details.dto.YaniRelatedAnimeResponseDto
import su.afk.yummy.tv.data.details.dto.YaniStudioDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniTrailersResponseDto
import su.afk.yummy.tv.domain.anime.model.AnimeRelationKind

class YaniAnimeApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getAnimeDetails(animeId: Int): YaniAnimeDetailsDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId").body()

    suspend fun getAnimeVideos(animeId: Int): YaniAnimeVideosDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/videos").body()

    suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): YaniRecommendationsDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/recommendations") {
            parameter("from_ai", fromAi)
            parameter("limit", 24)
        }.body()

    suspend fun getAnimeTrailers(animeId: Int): YaniTrailersResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/trailers").body()

    suspend fun getStudio(url: String): YaniStudioDetailsDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/studio/$url").body()

    suspend fun getDirector(id: Int): YaniDirectorDetailsDto =
        clientProvider.get().get("$YANI_BASE_URL/director/$id").body()

    suspend fun getGenre(id: Int): YaniGenreDetailsDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/genres/$id").body()

    suspend fun getRelatedAnime(
        kind: AnimeRelationKind,
        id: Int,
    ): YaniRelatedAnimeResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/anime") {
            parameter(
                when (kind) {
                    AnimeRelationKind.STUDIO -> "studio_ids"
                    AnimeRelationKind.DIRECTOR -> "director_ids"
                    AnimeRelationKind.GENRE -> "genres"
                },
                id,
            )
            parameter("sort", "rating")
            parameter("sort_forward", false)
            parameter("limit", 100)
            parameter("offset", 0)
        }.body()

    suspend fun ignoreAnimeRecommendation(animeId: Int): YaniRecommendationMutationResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/anime/$animeId/recommend").body()

    suspend fun restoreAnimeRecommendation(animeId: Int): YaniRecommendationMutationResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/anime/$animeId/recommend").body()

    suspend fun voteAnimeRecommendation(
        animeId: Int,
        similarAnimeId: Int,
        action: Int,
    ): YaniRecommendationVoteResponseDto =
        clientProvider.get().put(
            "$YANI_BASE_URL/anime/$animeId/recommend/$similarAnimeId/vote"
        ) {
            contentType(ContentType.Application.Json)
            setBody(YaniRecommendationVoteBodyDto(action))
        }.body()

    suspend fun deleteAnimeRecommendationVote(
        animeId: Int,
        similarAnimeId: Int,
    ): YaniRecommendationVoteResponseDto =
        clientProvider.get().delete(
            "$YANI_BASE_URL/anime/$animeId/recommend/$similarAnimeId/vote"
        ).body()
}

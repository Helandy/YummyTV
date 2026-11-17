package su.afk.yummy.tv.data.details.network

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.network.yani.YaniMirrorFallback
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
    private val fallback: YaniMirrorFallback,
) {
    /** Детали и видео при недоступности yani читаются из собственной копии на сервере YummyTV ([YaniMirrorFallback]). */
    suspend fun getAnimeDetails(animeId: Int): YaniAnimeDetailsDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/anime/$animeId").body() },
        mirror = { fallback.mirrorGet("/yani/anime/$animeId").body() },
    )

    suspend fun getAnimeVideos(animeId: Int): YaniAnimeVideosDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/videos").body() },
        mirror = { fallback.mirrorGet("/yani/anime/$animeId/videos").body() },
    )

    /** Общие рекомендации (`from_ai=true`) есть в копии; персональные (`from_ai=false`) только у yani. */
    suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): YaniRecommendationsDto {
        val primary: suspend () -> YaniRecommendationsDto = {
            clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/recommendations") {
                parameter("from_ai", fromAi)
                parameter("limit", 24)
            }.body()
        }
        if (!fromAi) return primary()
        return fallback.call(
            primary = primary,
            mirror = { fallback.mirrorGet("/yani/anime/$animeId/recommendations") { parameter("from_ai", true) }.body() },
        )
    }

    suspend fun getAnimeTrailers(animeId: Int): YaniTrailersResponseDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/trailers").body() },
        mirror = { fallback.mirrorGet("/yani/anime/$animeId/trailers").body() },
    )

    suspend fun getStudio(url: String): YaniStudioDetailsDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/anime/studio/$url").body() },
        mirror = { fallback.mirrorGet("/yani/anime/studio/$url").body() },
    )

    suspend fun getDirector(id: Int): YaniDirectorDetailsDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/director/$id").body() },
        mirror = { fallback.mirrorGet("/yani/director/$id").body() },
    )

    suspend fun getGenre(id: Int): YaniGenreDetailsDto = fallback.call(
        primary = { clientProvider.get().get("$YANI_BASE_URL/anime/genres/$id").body() },
        mirror = { fallback.mirrorGet("/yani/anime/genres/$id").body() },
    )

    suspend fun getRelatedAnime(
        kind: AnimeRelationKind,
        id: Int,
    ): YaniRelatedAnimeResponseDto {
        val params: HttpRequestBuilder.() -> Unit = {
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
        }
        return fallback.call(
            primary = { clientProvider.get().get("$YANI_BASE_URL/anime", params).body() },
            mirror = { fallback.mirrorGet("/yani/anime", params).body() },
        )
    }

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

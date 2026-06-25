package su.afk.yummy.tv.data.details.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideosDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationsDto
import su.afk.yummy.tv.data.details.dto.YaniTrailersResponseDto

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
}

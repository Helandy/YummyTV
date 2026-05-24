package su.afk.yummy.tv.data.details

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.domain.anime.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.AnimeRepository
import su.afk.yummy.tv.domain.anime.AnimeTrailer
import su.afk.yummy.tv.domain.anime.AnimeVideo

private const val ANIME_DETAILS_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_VIDEOS_TTL_MS = 60 * 60 * 1000L

class YaniAnimeRepository(
    private val client: HttpClient,
    private val cache: CacheStore,
    private val json: Json,
) : AnimeRepository {

    override suspend fun getAnimeDetails(animeId: Int): AnimeDetails = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "anime_details_v2_$animeId",
            ttlMs = ANIME_DETAILS_TTL_MS,
            serialize = { dto: YaniAnimeDetailsDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { client.get("$YANI_BASE_URL/anime/$animeId").body<YaniAnimeDetailsDto>() },
            isValid = { it.response.animeId != null },
        ).toAnimeDetails()
    }

    override suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo> = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "anime_videos_$animeId",
            ttlMs = ANIME_VIDEOS_TTL_MS,
            serialize = { dto: YaniAnimeVideosDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { client.get("$YANI_BASE_URL/anime/$animeId/videos").body<YaniAnimeVideosDto>() },
            isValid = { it.response.isNotEmpty() },
        ).response.map { it.toAnimeVideo() }
    }

    override suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): List<AnimeRecommendation> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.get("$YANI_BASE_URL/anime/$animeId/recommendations") {
                    parameter("from_ai", fromAi)
                    parameter("limit", 24)
                }.body<YaniRecommendationsDto>().response.mapNotNull { it.toAnimeRecommendation() }
            }.getOrElse { emptyList() }
        }

    override suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer> = withContext(Dispatchers.IO) {
        runCatching {
            client.get("$YANI_BASE_URL/anime/$animeId/trailers")
                .body<YaniTrailersResponseDto>()
                .response
                .map { AnimeTrailer(iframeUrl = it.iframeUrl.toHttpsUrl()) }
        }.getOrElse { emptyList() }
    }
}

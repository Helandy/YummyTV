package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideosDto
import su.afk.yummy.tv.data.details.mapper.toAnimeDetails
import su.afk.yummy.tv.data.details.mapper.toAnimeRecommendation
import su.afk.yummy.tv.data.details.mapper.toAnimeVideo
import su.afk.yummy.tv.data.details.mapper.toHttpsUrl
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo

private const val ANIME_DETAILS_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_VIDEOS_TTL_MS = 60 * 60 * 1000L

class YaniAnimeRepository(
    private val api: YaniAnimeApi,
    private val cache: CacheStore,
    private val json: Json,
) : AnimeRepository {

    override suspend fun getAnimeDetails(animeId: Int): AnimeDetails = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "anime_details_v2_$animeId",
            ttlMs = ANIME_DETAILS_TTL_MS,
            serialize = { dto: YaniAnimeDetailsDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { api.getAnimeDetails(animeId) },
            isValid = { it.response.animeId != null },
        ).toAnimeDetails()
    }

    override suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo> = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "anime_videos_$animeId",
            ttlMs = ANIME_VIDEOS_TTL_MS,
            serialize = { dto: YaniAnimeVideosDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { api.getAnimeVideos(animeId) },
            isValid = { it.response.isNotEmpty() },
        ).response.map { it.toAnimeVideo() }
    }

    override suspend fun getAnimeRecommendations(animeId: Int, fromAi: Boolean): List<AnimeRecommendation> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.getAnimeRecommendations(animeId, fromAi).response.mapNotNull { it.toAnimeRecommendation() }
            }.getOrElse { emptyList() }
        }

    override suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer> = withContext(Dispatchers.IO) {
        runCatching {
            api.getAnimeTrailers(animeId).response.map { AnimeTrailer(iframeUrl = it.iframeUrl.toHttpsUrl()) }
        }.getOrElse { emptyList() }
    }
}

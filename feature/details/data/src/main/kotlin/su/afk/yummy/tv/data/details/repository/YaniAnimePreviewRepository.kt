package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.mapper.toAnimePreview
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.anime.repository.AnimePreviewRepository

private const val ANIME_PREVIEW_TTL_MS = 24 * 60 * 60 * 1000L

class YaniAnimePreviewRepository(
    private val api: YaniAnimeApi,
    private val cache: CacheStore,
    private val json: Json,
) : AnimePreviewRepository {

    override suspend fun getAnimePreview(animeId: Int): AnimePreview = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "anime_details_$animeId",
            ttlMs = ANIME_PREVIEW_TTL_MS,
            serialize = { dto: YaniAnimeDetailsDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { api.getAnimeDetails(animeId) },
            isValid = { it.response.animeId != null },
        ).response.toAnimePreview()
    }
}

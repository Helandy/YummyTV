package su.afk.yummy.tv.data.details

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.domain.anime.AnimePreview
import su.afk.yummy.tv.domain.anime.AnimePreviewRepository

private const val ANIME_PREVIEW_TTL_MS = 24 * 60 * 60 * 1000L

class YaniAnimePreviewRepository(
    private val client: HttpClient,
    private val cache: CacheStore,
    private val json: Json,
) : AnimePreviewRepository {

    override suspend fun getAnimePreview(animeId: Int): AnimePreview = withContext(Dispatchers.IO) {
        val details = cache.getOrFetch(
            key = "anime_details_$animeId",
            ttlMs = ANIME_PREVIEW_TTL_MS,
            serialize = { dto: YaniAnimeDetailsDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { client.get("$YANI_BASE_URL/anime/$animeId").body<YaniAnimeDetailsDto>() },
            isValid = { it.response.animeId != null },
        ).response
        AnimePreview(
            trailerEmbedUrl = null,
            trailerStreamUrl = null,
            description = details.description,
            genres = details.genres.map { it.title }.filter { it.isNotBlank() },
            year = details.year?.takeIf { it > 0 },
            ageRating = details.minAge?.titleLong ?: details.minAge?.title,
            type = details.type?.name,
            views = details.views,
            season = details.season?.takeIf { it > 0 },
            screenshotUrls = details.randomScreenshots.mapNotNull {
                it.sizes.full?.toHttpsUrl() ?: it.sizes.small?.toHttpsUrl()
            },
        )
    }
}

package su.afk.yummy.tv.data.collection

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.domain.collection.CollectionAnimeItem
import su.afk.yummy.tv.domain.collection.CollectionDetail
import su.afk.yummy.tv.domain.collection.CollectionRepository

private const val COLLECTION_TTL_MS = 30 * 60 * 1000L

class YaniCollectionDetailRepository(
    private val client: HttpClient,
    private val cache: CacheStore,
    private val json: Json,
) : CollectionRepository {

    override suspend fun getCollection(id: Int): CollectionDetail =
        withContext(Dispatchers.IO) {
            cache.getOrFetch(
                key = "collection_$id",
                ttlMs = COLLECTION_TTL_MS,
                serialize = { dto: YaniCollectionDetailResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { client.get("$YANI_BASE_URL/collection/$id").body<YaniCollectionDetailResponseDto>() },
            ).response.toDomain()
        }

    private fun YaniCollectionDetailDto.toDomain(): CollectionDetail {
        val resolvedId = id ?: 0
        return CollectionDetail(
            id = resolvedId,
            title = title,
            description = description,
            views = views,
            posterUrl = posterPreviews.firstOrNull()?.toUrl(),
            animes = animes.mapNotNull { it.toDomain() },
        )
    }

    private fun YaniCollectionAnimeDto.toDomain(): CollectionAnimeItem? {
        val resolvedId = animeId ?: return null
        return CollectionAnimeItem(
            id = resolvedId,
            title = title,
            posterUrl = poster?.toUrl(),
            rating = rating?.average,
        )
    }

    private fun YaniCollectionPosterDto.toUrl(): String? =
        (big ?: medium ?: fullsize ?: small)?.let { url ->
            when {
                url.startsWith("//") -> "https:$url"
                url.startsWith("http://") -> url.replaceFirst("http://", "https://")
                else -> url
            }
        }
}

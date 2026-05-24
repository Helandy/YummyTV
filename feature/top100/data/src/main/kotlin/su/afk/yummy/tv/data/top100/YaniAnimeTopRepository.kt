package su.afk.yummy.tv.data.top100

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.domain.top100.AnimeTopPage
import su.afk.yummy.tv.domain.top100.AnimeTopRepository
import su.afk.yummy.tv.domain.top100.AnimeTopType

private const val ANIME_TOP_TTL_MS = 6 * 60 * 60 * 1000L

class YaniAnimeTopRepository(
    private val client: HttpClient,
    private val cache: CacheStore,
    private val json: Json,
) : AnimeTopRepository {

    override suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        withContext(Dispatchers.IO) {
            val response = cache.getOrFetch(
                key = "anime_top_${type.apiValue}_${limit}_$offset",
                ttlMs = ANIME_TOP_TTL_MS,
                serialize = { dto: YaniAnimeTopListDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = {
                    client.get("$YANI_BASE_URL/anime") {
                        parameter("sort", "top")
                        parameter("types", type.apiValue)
                        parameter("limit", limit)
                        parameter("offset", offset)
                        parameter("sort_forward", true)
                        parameter("from_year", 1900)
                    }.body<YaniAnimeTopListDto>()
                },
            ).response
            AnimeTopPage(
                items = response.mapNotNull { it.toAnimeTopItem() },
                nextOffset = offset + response.size,
                canLoadMore = response.size >= limit,
            )
        }
}

package su.afk.yummy.tv.data.home

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.domain.home.HomeFeed
import su.afk.yummy.tv.domain.home.HomeFeedRepository

private const val FEED_TTL_MS = 60 * 60 * 1000L

class YaniHomeFeedRepository(
    private val client: HttpClient,
    private val cache: CacheStore,
    private val json: Json,
    private val stringProvider: StringProvider,
) : HomeFeedRepository {

    override suspend fun getHomeFeed(): HomeFeed = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "feed",
            ttlMs = FEED_TTL_MS,
            serialize = { dto: YaniFeedDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { client.get("$YANI_BASE_URL/feed").body<YaniFeedDto>() },
        ).toHomeFeed(stringProvider)
    }
}

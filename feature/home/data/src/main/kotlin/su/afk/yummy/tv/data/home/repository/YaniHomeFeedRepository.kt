package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.mapper.toHomeFeed
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

private const val FEED_TTL_MS = 60 * 60 * 1000L

class YaniHomeFeedRepository(
    private val api: YaniHomeApi,
    private val cache: CacheStore,
    private val json: Json,
    private val stringProvider: StringProvider,
) : HomeFeedRepository {

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        cache.getOrFetch(
            key = "feed",
            ttlMs = FEED_TTL_MS,
            serialize = { dto: YaniFeedDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { api.getFeed() },
            forceRefresh = forceRefresh,
        ).toHomeFeed(stringProvider)
    }
}

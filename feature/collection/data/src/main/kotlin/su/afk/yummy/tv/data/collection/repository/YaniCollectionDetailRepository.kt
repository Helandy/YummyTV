package su.afk.yummy.tv.data.collection.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailResponseDto
import su.afk.yummy.tv.data.collection.mapper.toDomain
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository

private const val COLLECTION_TTL_MS = 30 * 60 * 1000L

class YaniCollectionDetailRepository(
    private val api: YaniCollectionApi,
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
                fetch = { api.getCollection(id) },
            ).response.toDomain()
        }
}

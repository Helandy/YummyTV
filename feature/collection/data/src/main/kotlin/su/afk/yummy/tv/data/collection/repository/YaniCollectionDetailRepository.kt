package su.afk.yummy.tv.data.collection.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailResponseDto
import su.afk.yummy.tv.data.collection.mapper.toDomain
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository

private const val COLLECTION_TTL_MS = 24 * 60 * 60 * 1000L

class YaniCollectionDetailRepository(
    private val api: YaniCollectionApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : CollectionRepository {

    override suspend fun getCollection(id: Int): CollectionDetail =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            cache.getOrFetch(
                key = "collection_$id".withYaniContentLanguage(language),
                ttlMs = COLLECTION_TTL_MS,
                serialize = { dto: YaniCollectionDetailResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { api.getCollection(id) },
            ).response.toDomain()
        }
}

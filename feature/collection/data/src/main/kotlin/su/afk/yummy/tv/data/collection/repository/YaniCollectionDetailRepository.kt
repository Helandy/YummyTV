package su.afk.yummy.tv.data.collection.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.collection.CollectionStorageStore
import su.afk.yummy.tv.core.storage.collection.isFresh
import su.afk.yummy.tv.data.collection.mapper.toDomain
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.data.collection.storage.mapper.toCollectionDetail
import su.afk.yummy.tv.data.collection.storage.mapper.toCollectionDetailCache
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository

private const val COLLECTION_TTL_MS = 24 * 60 * 60 * 1000L

class YaniCollectionDetailRepository(
    private val api: YaniCollectionApi,
    private val collectionStorage: CollectionStorageStore,
    private val settingsStore: SettingsStore,
) : CollectionRepository {

    override suspend fun getCollection(id: Int): CollectionDetail =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = collectionStorage.getCollection(id, languageCode)
            if (stored?.isFresh(COLLECTION_TTL_MS) == true) {
                return@withContext stored.toCollectionDetail()
            }

            try {
                fetchCollection(id, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toCollectionDetail()
                    ?: throw error
            }
        }

    private suspend fun fetchCollection(id: Int, languageCode: String): CollectionDetail {
        val collection = api.getCollection(id).response.toDomain(fallbackId = id)
        collectionStorage.saveCollection(
            collection.toCollectionDetailCache(
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return collection
    }
}

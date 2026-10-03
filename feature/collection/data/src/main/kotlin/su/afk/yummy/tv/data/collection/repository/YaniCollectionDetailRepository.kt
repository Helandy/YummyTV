package su.afk.yummy.tv.data.collection.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.collection.CollectionDetailCache
import su.afk.yummy.tv.core.storage.collection.CollectionStorage
import su.afk.yummy.tv.core.storage.collection.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.collection.dto.YaniCollectionVoteBodyDto
import su.afk.yummy.tv.data.collection.dto.YaniCreateCollectionBodyDto
import su.afk.yummy.tv.data.collection.dto.YaniUpdateCollectionBodyDto
import su.afk.yummy.tv.data.collection.mapper.toDomain
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.data.collection.storage.mapper.toCollectionCatalogPageCache
import su.afk.yummy.tv.data.collection.storage.mapper.toCollectionDetail
import su.afk.yummy.tv.data.collection.storage.mapper.toCollectionDetailCache
import su.afk.yummy.tv.data.collection.storage.mapper.toCollectionSummaryPage
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.domain.collection.model.CollectionVoteResult
import su.afk.yummy.tv.domain.collection.model.CreateCollectionRequest
import su.afk.yummy.tv.domain.collection.model.UpdateCollectionRequest
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository

private const val COLLECTION_TTL_MS = 60 * 1000L
private const val COLLECTION_CATALOG_TTL_MS = 60 * 1000L

class YaniCollectionDetailRepository(
    private val api: YaniCollectionApi,
    private val collectionStorage: CollectionStorage,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : CollectionRepository {

    override suspend fun getCollection(id: Int): CollectionDetail =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { collectionStorage.getCollection(id, languageCode) },
                isFresh = { it.isFresh(COLLECTION_TTL_MS) && it.entry.ownerId > 0 },
                toDomain = { it.toCollectionDetail() },
                fetchAndSave = { fetchCollection(id, languageCode) },
            )
        }

    override suspend fun getCollections(limit: Int, offset: Int): CollectionSummaryPage =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            val pageKey = catalogPageKey(languageCode, limit, offset)
            offlineFirstCache(
                read = { collectionStorage.getCatalogPage(pageKey) },
                isFresh = { it.isFresh(COLLECTION_CATALOG_TTL_MS) },
                toDomain = { it.toCollectionSummaryPage() },
                fetchAndSave = {
                    val response = api.getCollections(limit, offset).response
                    val cache = response.toCollectionCatalogPageCache(
                        pageKey = pageKey,
                        language = languageCode,
                        limit = limit,
                        offset = offset,
                        responseSize = response.size,
                        cachedAt = System.currentTimeMillis(),
                    )
                    collectionStorage.saveCatalogPage(cache)
                    cache
                },
            )
        }

    override suspend fun createCollection(request: CreateCollectionRequest): Int =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            api.createCollection(
                YaniCreateCollectionBodyDto(
                    isPublic = request.isPublic,
                    language = languageCode,
                    description = request.description,
                    title = request.title,
                )
            ).response.id.also { id ->
                check(id > 0) { "Collection creation returned an invalid id" }
                invalidateCollectionLists()
            }
        }

    override suspend fun updateCollection(id: Int, request: UpdateCollectionRequest): Boolean =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            val updated = api.updateCollection(
                id = id,
                body = YaniUpdateCollectionBodyDto(
                    isPublic = request.isPublic,
                    description = request.description,
                    title = request.title,
                ),
            ).response
            if (updated) {
                collectionStorage.getCollection(id, languageCode)?.let { stored ->
                    collectionStorage.saveCollection(
                        stored.copy(
                            entry = stored.entry.copy(
                                title = request.title,
                                description = request.description,
                                isPublic = request.isPublic,
                                cachedAt = System.currentTimeMillis(),
                            ),
                        )
                    )
                }
                invalidateCollectionLists()
            }
            updated
        }

    override suspend fun deleteCollection(id: Int): Boolean =
        withContext(Dispatchers.IO) {
            val deleted = api.deleteCollection(id).response
            if (deleted) {
                collectionStorage.deleteCollection(id)
                invalidateCollectionLists()
            }
            deleted
        }

    override suspend fun voteCollection(id: Int, vote: CollectionVote): CollectionVoteResult =
        withContext(Dispatchers.IO) {
            require(vote != CollectionVote.NEUTRAL)
            val languageCode = settingsStore.currentLanguageCode()
            val result = api.voteCollection(id, YaniCollectionVoteBodyDto(vote.apiValue))
                .response
                .toDomain()
            collectionStorage.updateCollectionVote(
                collectionId = id,
                language = languageCode,
                likes = result.likes,
                dislikes = result.dislikes,
                vote = vote.apiValue,
            )
            result
        }

    override suspend fun removeCollectionVote(id: Int): CollectionVoteResult =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            val result = api.removeCollectionVote(id).response.toDomain()
            collectionStorage.updateCollectionVote(
                collectionId = id,
                language = languageCode,
                likes = result.likes,
                dislikes = result.dislikes,
                vote = CollectionVote.NEUTRAL.apiValue,
            )
            result
        }

    private suspend fun fetchCollection(id: Int, languageCode: String): CollectionDetailCache {
        val cache = api.getCollection(id).response.toCollectionDetailCache(
            fallbackId = id,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        collectionStorage.saveCollection(cache)
        return cache
    }

    private fun catalogPageKey(language: String, limit: Int, offset: Int): String =
        "$language:$limit:$offset"

    private suspend fun invalidateCollectionLists() {
        collectionStorage.invalidateCatalog()
        accountStorage.invalidateCollections()
    }
}

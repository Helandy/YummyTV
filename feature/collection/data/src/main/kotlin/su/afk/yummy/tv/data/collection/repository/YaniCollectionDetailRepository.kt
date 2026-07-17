package su.afk.yummy.tv.data.collection.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.collection.CollectionStorageStore
import su.afk.yummy.tv.core.storage.collection.isFresh
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
    private val collectionStorage: CollectionStorageStore,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : CollectionRepository {

    override suspend fun getCollection(id: Int): CollectionDetail =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = collectionStorage.getCollection(id, languageCode)
            if (stored?.isFresh(COLLECTION_TTL_MS) == true && stored.entry.ownerId > 0) {
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

    override suspend fun getCollections(limit: Int, offset: Int): CollectionSummaryPage =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val pageKey = catalogPageKey(languageCode, limit, offset)
            val stored = collectionStorage.getCatalogPage(pageKey)
            if (stored?.isFresh(COLLECTION_CATALOG_TTL_MS) == true) {
                return@withContext stored.toCollectionSummaryPage()
            }

            try {
                val response = api.getCollections(limit, offset).response
                val cache = response.toCollectionCatalogPageCache(
                    pageKey = pageKey,
                    language = languageCode,
                    limit = limit,
                    offset = offset,
                    responseSize = response.size,
                    cachedAt = System.currentTimeMillis(),
                )
                collectionStorage.saveCatalogPage(
                    cache
                )
                cache.toCollectionSummaryPage()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toCollectionSummaryPage()
                    ?: throw error
            }
        }

    override suspend fun createCollection(request: CreateCollectionRequest): Int =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
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
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
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
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
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
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
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

    private suspend fun fetchCollection(id: Int, languageCode: String): CollectionDetail {
        val cache = api.getCollection(id).response.toCollectionDetailCache(
            fallbackId = id,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        collectionStorage.saveCollection(cache)
        return cache.toCollectionDetail()
    }

    private fun catalogPageKey(language: String, limit: Int, offset: Int): String =
        "$language:$limit:$offset"

    private suspend fun invalidateCollectionLists() {
        collectionStorage.invalidateCatalog()
        accountStorage.invalidateCollections()
    }
}

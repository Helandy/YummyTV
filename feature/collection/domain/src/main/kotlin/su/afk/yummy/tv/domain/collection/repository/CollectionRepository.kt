package su.afk.yummy.tv.domain.collection.repository

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummary

interface CollectionRepository {
    suspend fun getCollection(id: Int): CollectionDetail
    suspend fun getCollections(limit: Int, offset: Int): List<CollectionSummary>
}

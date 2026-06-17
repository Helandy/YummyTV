package su.afk.yummy.tv.domain.collection.repository

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage

interface CollectionRepository {
    suspend fun getCollection(id: Int): CollectionDetail
    suspend fun getCollections(limit: Int, offset: Int): CollectionSummaryPage
}

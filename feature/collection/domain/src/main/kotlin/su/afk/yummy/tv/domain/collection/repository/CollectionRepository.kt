package su.afk.yummy.tv.domain.collection.repository

import su.afk.yummy.tv.domain.collection.model.CollectionDetail

interface CollectionRepository {
    suspend fun getCollection(id: Int): CollectionDetail
}

package su.afk.yummy.tv.domain.collection.repository

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.domain.collection.model.CollectionVoteResult
import su.afk.yummy.tv.domain.collection.model.CreateCollectionRequest
import su.afk.yummy.tv.domain.collection.model.UpdateCollectionRequest

interface CollectionRepository {
    suspend fun getCollection(id: Int): CollectionDetail
    suspend fun getCollections(limit: Int, offset: Int): CollectionSummaryPage
    suspend fun createCollection(request: CreateCollectionRequest): Int
    suspend fun updateCollection(id: Int, request: UpdateCollectionRequest): Boolean
    suspend fun deleteCollection(id: Int): Boolean
    suspend fun voteCollection(id: Int, vote: CollectionVote): CollectionVoteResult
    suspend fun removeCollectionVote(id: Int): CollectionVoteResult
}

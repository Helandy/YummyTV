package su.afk.yummy.tv.domain.collection.repository

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.domain.collection.model.CollectionVoteResult

interface CollectionRepository {
    suspend fun getCollection(id: Int): CollectionDetail
    suspend fun getCollections(limit: Int, offset: Int): CollectionSummaryPage
    suspend fun voteCollection(id: Int, vote: CollectionVote): CollectionVoteResult
    suspend fun removeCollectionVote(id: Int): CollectionVoteResult
}

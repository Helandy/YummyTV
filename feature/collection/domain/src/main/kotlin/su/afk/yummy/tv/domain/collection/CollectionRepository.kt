package su.afk.yummy.tv.domain.collection

interface CollectionRepository {
    suspend fun getCollection(id: Int): CollectionDetail
}

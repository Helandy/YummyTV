package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository

/** Loads full details for a collection by id. */
class GetCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Int): CollectionDetail = repository.getCollection(id)
}

package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

/** Loads full details for a collection by id. */
class GetCollectionUseCase @Inject constructor(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Int): CollectionDetail = repository.getCollection(id)
}

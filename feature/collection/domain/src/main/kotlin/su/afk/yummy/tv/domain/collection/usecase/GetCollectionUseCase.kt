package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

/** Загружает полные данные коллекции по её идентификатору. */
class GetCollectionUseCase @Inject constructor(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Int): CollectionDetail = repository.getCollection(id)
}

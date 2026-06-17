package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.model.CollectionSummaryPage
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

/** Загружает публичные коллекции с постраничной навигацией. */
class GetCollectionsUseCase @Inject constructor(private val repository: CollectionRepository) {
    suspend operator fun invoke(limit: Int, offset: Int): CollectionSummaryPage =
        repository.getCollections(limit, offset)
}

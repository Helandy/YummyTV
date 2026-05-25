package su.afk.yummy.tv.domain.collection

/** Loads full details for a collection by id. */
class GetCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Int): CollectionDetail = repository.getCollection(id)
}

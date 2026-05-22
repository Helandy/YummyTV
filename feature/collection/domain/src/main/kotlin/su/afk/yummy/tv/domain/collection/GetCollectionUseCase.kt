package su.afk.yummy.tv.domain.collection

class GetCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Int): CollectionDetail = repository.getCollection(id)
}

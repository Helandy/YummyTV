package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.CollectionMutationNotifier
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

/** Удаляет коллекцию и уведомляет наблюдателей об успешном изменении. */
class DeleteCollectionUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val mutationNotifier: CollectionMutationNotifier,
) {
    suspend operator fun invoke(id: Int): Boolean =
        repository.deleteCollection(id).also { deleted ->
            if (deleted) mutationNotifier.notifyChanged()
        }
}

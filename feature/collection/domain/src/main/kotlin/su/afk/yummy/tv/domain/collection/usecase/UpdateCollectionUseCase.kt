package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.CollectionMutationNotifier
import su.afk.yummy.tv.domain.collection.model.UpdateCollectionRequest
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

/** Обновляет коллекцию и уведомляет наблюдателей об успешном изменении. */
class UpdateCollectionUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val mutationNotifier: CollectionMutationNotifier,
) {
    suspend operator fun invoke(id: Int, request: UpdateCollectionRequest): Boolean =
        repository.updateCollection(id, request).also { updated ->
            if (updated) mutationNotifier.notifyChanged()
        }
}

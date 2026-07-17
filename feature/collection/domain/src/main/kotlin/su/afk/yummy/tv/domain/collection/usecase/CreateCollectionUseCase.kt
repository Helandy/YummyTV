package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.CollectionMutationNotifier
import su.afk.yummy.tv.domain.collection.model.CreateCollectionRequest
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

class CreateCollectionUseCase @Inject constructor(
    private val repository: CollectionRepository,
    private val mutationNotifier: CollectionMutationNotifier,
) {
    suspend operator fun invoke(request: CreateCollectionRequest): Int =
        repository.createCollection(request).also { mutationNotifier.notifyChanged() }
}

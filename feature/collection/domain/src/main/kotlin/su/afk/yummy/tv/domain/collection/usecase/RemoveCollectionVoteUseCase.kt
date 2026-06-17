package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

class RemoveCollectionVoteUseCase @Inject constructor(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(collectionId: Int) =
        repository.removeCollectionVote(collectionId)
}

package su.afk.yummy.tv.domain.collection.usecase

import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Inject

/** Сохраняет реакцию текущего пользователя на выбранную коллекцию. */
class VoteCollectionUseCase @Inject constructor(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(collectionId: Int, vote: CollectionVote) =
        repository.voteCollection(collectionId, vote)
}

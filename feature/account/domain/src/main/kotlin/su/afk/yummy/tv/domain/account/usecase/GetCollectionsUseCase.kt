package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Loads general collection summaries from the account extras API. */
class GetCollectionsUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(limit: Int = 40, offset: Int = 0): List<AnimeCollectionSummary> =
        repository.getCollections(limit, offset)
}

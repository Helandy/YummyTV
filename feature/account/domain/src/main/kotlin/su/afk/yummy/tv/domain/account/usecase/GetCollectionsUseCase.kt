package su.afk.yummy.tv.domain.account

/** Loads general collection summaries from the account extras API. */
class GetCollectionsUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(limit: Int = 40, offset: Int = 0): List<AnimeCollectionSummary> =
        repository.getCollections(limit, offset)
}

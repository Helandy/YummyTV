package su.afk.yummy.tv.domain.account

/** Loads collection summaries that contain the selected anime. */
class GetAnimeCollectionsUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int, limit: Int = 20, offset: Int = 0): List<AnimeCollectionSummary> =
        repository.getCollections(animeId, limit, offset)
}

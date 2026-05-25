package su.afk.yummy.tv.domain.account

/** Loads list membership statistics for an anime. */
class GetAnimeListStatsUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): AnimeListStats = repository.getListStats(animeId)
}

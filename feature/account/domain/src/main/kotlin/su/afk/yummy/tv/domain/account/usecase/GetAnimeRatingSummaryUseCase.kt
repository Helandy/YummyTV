package su.afk.yummy.tv.domain.account

/** Loads public rating counters and averages for an anime. */
class GetAnimeRatingSummaryUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): AnimeRatingSummary = repository.getRatingSummary(animeId)
}

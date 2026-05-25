package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

/** Loads public rating counters and averages for an anime. */
class GetAnimeRatingSummaryUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): AnimeRatingSummary = repository.getRatingSummary(animeId)
}

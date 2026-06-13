package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Loads list membership statistics for an anime. */
class GetAnimeListStatsUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): AnimeListStats = repository.getListStats(animeId)
}

package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Читает кешированную статистику списков без сетевого обновления. */
class GetCachedAnimeListStatsUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): AnimeListStats? =
        repository.getCachedListStats(animeId)
}

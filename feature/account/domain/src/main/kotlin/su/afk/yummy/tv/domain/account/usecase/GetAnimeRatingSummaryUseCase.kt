package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Загружает публичную сводку оценок для выбранного аниме. */
class GetAnimeRatingSummaryUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int, forceRefresh: Boolean = false): AnimeRatingSummary = repository.getRatingSummary(animeId, forceRefresh)
}

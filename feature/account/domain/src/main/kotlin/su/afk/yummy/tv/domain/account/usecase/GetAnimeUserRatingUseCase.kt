package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Loads the current user's rating for an anime when it exists. */
class GetAnimeUserRatingUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): Int? = repository.getUserRating(animeId)
}

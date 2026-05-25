package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

/** Deletes the current user's rating for an anime. */
class DeleteAnimeRatingUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int) = repository.deleteRating(animeId)
}

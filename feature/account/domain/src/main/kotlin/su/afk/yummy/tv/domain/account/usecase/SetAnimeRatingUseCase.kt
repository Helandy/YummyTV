package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Saves the current user's rating for an anime. */
class SetAnimeRatingUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int, rating: Int) = repository.setRating(animeId, rating)
}

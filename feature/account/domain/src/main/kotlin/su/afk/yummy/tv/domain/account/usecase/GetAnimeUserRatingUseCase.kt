package su.afk.yummy.tv.domain.account

/** Loads the current user's rating for an anime when it exists. */
class GetAnimeUserRatingUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): Int? = repository.getUserRating(animeId)
}

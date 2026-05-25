package su.afk.yummy.tv.domain.account

/** Deletes the current user's rating for an anime. */
class DeleteAnimeRatingUseCase(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int) = repository.deleteRating(animeId)
}

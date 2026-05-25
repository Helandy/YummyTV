package su.afk.yummy.tv.domain.anime

/** Loads trailer iframe links for an anime. */
class GetAnimeTrailersUseCase(private val repo: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeTrailer> = repo.getAnimeTrailers(animeId)
}

package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository

/** Loads trailer iframe links for an anime. */
class GetAnimeTrailersUseCase(private val repo: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeTrailer> = repo.getAnimeTrailers(animeId)
}

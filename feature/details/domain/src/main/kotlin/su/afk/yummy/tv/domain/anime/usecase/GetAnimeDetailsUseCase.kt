package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository

/** Loads the full details page data for an anime. */
class GetAnimeDetailsUseCase(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimeDetails =
        animeRepository.getAnimeDetails(animeId)
}

package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Loads the full details page data for an anime. */
class GetAnimeDetailsUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimeDetails =
        animeRepository.getAnimeDetails(animeId)
}

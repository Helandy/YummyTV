package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository

/** Loads anime recommendations for the selected title. */
class GetAnimeRecommendationsUseCase(private val repo: AnimeRepository) {
    suspend operator fun invoke(animeId: Int, fromAi: Boolean): List<AnimeRecommendation> =
        repo.getAnimeRecommendations(animeId, fromAi)
}

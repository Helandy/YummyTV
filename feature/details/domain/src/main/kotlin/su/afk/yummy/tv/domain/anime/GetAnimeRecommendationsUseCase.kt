package su.afk.yummy.tv.domain.anime

class GetAnimeRecommendationsUseCase(private val repo: AnimeRepository) {
    suspend operator fun invoke(animeId: Int, fromAi: Boolean): List<AnimeRecommendation> =
        repo.getAnimeRecommendations(animeId, fromAi)
}

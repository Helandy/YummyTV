package su.afk.yummy.tv.domain.anime

class GetAnimeDetailsUseCase(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimeDetails =
        animeRepository.getAnimeDetails(animeId)
}

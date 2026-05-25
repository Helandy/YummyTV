package su.afk.yummy.tv.domain.anime

/** Loads playable video entries for an anime. */
class GetAnimeVideosUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeVideo> = repository.getAnimeVideos(animeId)
}

package su.afk.yummy.tv.domain.anime

class GetAnimePreviewUseCase(
    private val repository: AnimePreviewRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimePreview = repository.getAnimePreview(animeId)
}

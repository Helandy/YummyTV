package su.afk.yummy.tv.domain.anime

/** Loads compact preview data for an anime. */
class GetAnimePreviewUseCase(
    private val repository: AnimePreviewRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimePreview = repository.getAnimePreview(animeId)
}

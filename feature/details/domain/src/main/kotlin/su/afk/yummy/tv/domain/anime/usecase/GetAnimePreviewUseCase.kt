package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.anime.repository.AnimePreviewRepository

/** Loads compact preview data for an anime. */
class GetAnimePreviewUseCase(
    private val repository: AnimePreviewRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimePreview = repository.getAnimePreview(animeId)
}

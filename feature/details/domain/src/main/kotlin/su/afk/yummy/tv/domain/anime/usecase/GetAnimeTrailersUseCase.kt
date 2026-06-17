package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Загружает iframe-ссылки трейлеров для выбранного аниме. */
class GetAnimeTrailersUseCase @Inject constructor(private val repo: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeTrailer> = repo.getAnimeTrailers(animeId)
}

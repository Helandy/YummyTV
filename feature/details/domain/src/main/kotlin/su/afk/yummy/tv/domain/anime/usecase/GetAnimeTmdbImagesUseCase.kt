package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.core.model.anime.AnimeTmdbImages
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Картинки тайтла из TMDB: задники для шапки деталки, кадры серии 1 для скриншотов и кадры всех серий для превью.
 * Пустая модель, если у тайтла нет соответствия в TMDB или сервер недоступен.
 */
class GetAnimeTmdbImagesUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimeTmdbImages = animeRepository.getAnimeTmdbImages(animeId)
}

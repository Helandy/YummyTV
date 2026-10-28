package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Ссылки на превью серий по номеру серии (кадры MyAnimeList через Tenrai). Пустая мапа,
 * если у тайтла нет MAL id или данных нет.
 */
class GetAnimeEpisodeThumbnailsUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): Map<String, String> =
        animeRepository.getAnimeEpisodeThumbnails(animeId)
}

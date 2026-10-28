package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Пометки «филлер» и «рекап» по номеру серии (данные MyAnimeList); в мапе только серии
 * хотя бы с одной пометкой. Пустая мапа, если у тайтла нет MAL id или данных нет.
 */
class GetAnimeEpisodeFlagsUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): Map<String, EpisodeFlags> =
        animeRepository.getAnimeEpisodeFlags(animeId)
}

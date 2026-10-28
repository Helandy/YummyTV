package su.afk.yummy.tv.domain.anime.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Пометки «филлер» и «рекап» по номеру серии (данные MyAnimeList); в мапе только серии
 * хотя бы с одной пометкой. У длинных тайтлов сначала приходят пометки первой страницы серий, затем полные;
 * пустая мапа, если у тайтла нет MAL id или данных нет.
 */
class ObserveAnimeEpisodeFlagsUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    operator fun invoke(animeId: Int): Flow<Map<String, EpisodeFlags>> =
        animeRepository.observeAnimeEpisodeFlags(animeId)
}

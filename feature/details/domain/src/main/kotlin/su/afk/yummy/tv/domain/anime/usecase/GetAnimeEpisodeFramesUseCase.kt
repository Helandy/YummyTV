package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Кадры серий для ряда «Скриншоты» деталки: кадры серии 1 из TMDB и кадры первых серий из Tenrai. Скриншоты Yani
 * третьим источником добавляет экран из деталей тайтла. Пустая модель, если у тайтла нет ни TMDB, ни Tenrai.
 */
class GetAnimeEpisodeFramesUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimeEpisodeFrames = animeRepository.getAnimeEpisodeFrames(animeId)
}

package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

/** Возвращает локальный прогресс просмотра всех серий тайтла, которые пользователь открывал. */
class GetAnimeWatchProgressUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
) {
    suspend operator fun invoke(animeId: Int): List<AnimeWatchProgress> =
        repository.getByAnime(animeId)
}

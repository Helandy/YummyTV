package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

/** Возвращает локальный прогресс просмотра серии тайтла или null, если серия ещё не открывалась. */
class GetWatchProgressUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
) {
    suspend operator fun invoke(animeId: Int, episode: String): AnimeWatchProgress? =
        repository.get(animeId, episode)
}

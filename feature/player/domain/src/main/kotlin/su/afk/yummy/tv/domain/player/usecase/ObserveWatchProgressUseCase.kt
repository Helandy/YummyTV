package su.afk.yummy.tv.domain.player.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

/** Наблюдает за локальным прогрессом просмотра всех серий, чтобы показывать его в списках без захода в плеер. */
class ObserveWatchProgressUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
) {
    operator fun invoke(): Flow<List<AnimeWatchProgress>> = repository.observeAll()
}

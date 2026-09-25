package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

/** Возвращает локальный прогресс всех серий, где просмотр продвинулся достаточно, чтобы его показывать. */
class GetMeaningfulVideoProgressUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
) {
    suspend operator fun invoke(): List<AnimeWatchProgress> = repository.allMeaningfulVideoProgress()
}

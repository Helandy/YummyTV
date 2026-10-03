package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import javax.inject.Inject

/** Скрывает тайтл из Continue Watching с текущего момента, пока по нему не появится новая активность. */
class SuppressContinueWatchingDisplayUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(animeId: Int) {
        repository.suppressContinueWatchingDisplay(
            animeId = animeId,
            suppressedAt = clock.nowMillis(),
        )
    }
}

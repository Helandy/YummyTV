package su.afk.yummy.tv.domain.watchlater.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem
import su.afk.yummy.tv.domain.watchlater.repository.WatchLaterRepository
import javax.inject.Inject

/** Откладывает серию «на потом». Повторное добавление просто обновляет запись. */
class AddWatchLaterEpisodeUseCase @Inject constructor(
    private val repository: WatchLaterRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        animeId: Int,
        episode: String,
        animeTitle: String,
        posterUrl: String,
        screenshotUrl: String,
    ) {
        if (animeId <= 0 || episode.isBlank()) return
        repository.add(
            WatchLaterItem(
                animeId = animeId,
                episode = episode,
                animeTitle = animeTitle,
                posterUrl = posterUrl,
                screenshotUrl = screenshotUrl,
                addedAt = clock.nowMillis(),
            )
        )
    }
}

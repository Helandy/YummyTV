package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import javax.inject.Inject

/** Сохраняет прогресс просмотра видео без пользовательского уведомления об ошибках. */
class SaveVideoWatchProgressUseCase @Inject constructor(
    private val repository: VideoWatchesRepository,
) {
    suspend operator fun invoke(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        repository.markWatched(videoId, timeSeconds, durationSeconds)
}

package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.domain.player.utils.nextActivityUpdatedAt
import javax.inject.Inject

/**
 * Запоминает серию как цель Continue Watching без записи позиции — например, когда плеер открыт,
 * но воспроизведение ещё не началось.
 */
class SaveContinueTargetUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
    private val clock: AppClock,
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) {
        repository.saveContinueTarget(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            updatedAt = nextActivityUpdatedAt(repository, animeId, episode, clock.nowMillis()),
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )
    }
}

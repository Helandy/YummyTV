package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.domain.player.utils.nextActivityUpdatedAt
import javax.inject.Inject

/**
 * Сохраняет локальный прогресс просмотра серии. Время активности строго возрастает относительно
 * уже сохранённой записи серии, чтобы Continue Watching упорядочивался стабильно даже при записях
 * в пределах одной миллисекунды.
 */
class SaveWatchProgressUseCase @Inject constructor(
    private val repository: WatchProgressRepository,
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        animeId: Int,
        episode: String,
        videoId: Int,
        episodeUrl: String,
        positionMs: Long,
        durationMs: Long,
        animeTitle: String,
        posterUrl: String,
        playerName: String,
        dubbing: String,
        screenshotUrl: String,
    ) {
        repository.save(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = nextActivityUpdatedAt(repository, animeId, episode),
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )
    }
}

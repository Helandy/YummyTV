package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.usecase.MarkVideoWatchedUseCase
import javax.inject.Inject

/** Persists local watch progress and mirrors watched completion to the signed-in account. */
internal class PlayerProgressHandler @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
    private val markVideoWatched: MarkVideoWatchedUseCase,
) {
    private val markedWatchedVideoIds = mutableSetOf<Int>()
    private val markingWatchedVideoIds = mutableSetOf<Int>()

    suspend fun saveProgress(
        context: PlayerProgressContext,
        snapshot: PlayerProgressSnapshot,
    ) {
        if (context.animeId == 0 || snapshot.episode.isBlank() || snapshot.durationMs <= 0) return

        watchProgressStore.save(
            animeId = context.animeId,
            episode = snapshot.episode,
            videoId = snapshot.videoId,
            episodeUrl = snapshot.episodeUrl,
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
            animeTitle = context.animeTitle,
            posterUrl = context.posterUrl,
            playerName = snapshot.playerName,
            dubbing = snapshot.dubbing,
            screenshotUrl = snapshot.screenshotUrl,
        )

        val videoId = snapshot.videoId
        val watchedEnough = videoId > 0 &&
                WatchProgressStore.isWatchedProgress(snapshot.positionMs, snapshot.durationMs)
        if (!watchedEnough) return
        if (videoId in markedWatchedVideoIds || !markingWatchedVideoIds.add(videoId)) return

        runCatching {
            markVideoWatched(
                videoId = videoId,
                timeSeconds = (snapshot.positionMs / 1000L).toInt(),
                durationSeconds = (snapshot.durationMs / 1000L).toInt(),
            )
        }.onSuccess {
            markedWatchedVideoIds += videoId
        }.also {
            markingWatchedVideoIds -= videoId
        }
    }
}

/** Screen-level metadata needed to store a progress entry. */
internal data class PlayerProgressContext(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
)

package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.utils.episodeNumberOrNull
import su.afk.yummy.tv.domain.watching.mapper.toContinueWatchingPlaybackVideo
import javax.inject.Inject

internal class ServerContinueProgressSelector @Inject constructor() {

    fun select(videos: List<AnimeVideo>): ServerContinueProgress? =
        videos.mapNotNull { video -> video.toServerProgress() }
            .maxWithOrNull(
                compareBy<ServerContinueProgress> { it.updatedAt }
                    .thenBy(ServerContinueProgress::positionMs)
                    .thenBy {
                        it.video.episode.episodeNumberOrNull() ?: Double.NEGATIVE_INFINITY
                    },
            )

    private companion object {
        fun AnimeVideo.toServerProgress(): ServerContinueProgress? {
            val positionSeconds = watchedEndTimeSeconds
                ?.takeIf { it >= 0 }
                ?: return null
            val updatedAtSeconds = watchedDateSeconds
                ?.takeIf { it > 0L }
                ?: return null
            val durationSeconds = durationSeconds
                ?.takeIf { it > 0 }
                ?: return null
            val positionMs = positionSeconds * 1_000L
            val durationMs = durationSeconds * 1_000L
            if (!isMeaningfulProgress(positionMs, durationMs)) return null
            if (isWatchedProgress(positionMs, durationMs)) return null
            return ServerContinueProgress(
                video = toContinueWatchingPlaybackVideo(),
                positionMs = positionMs,
                updatedAt = updatedAtSeconds * 1_000L,
            )
        }

        fun isMeaningfulProgress(positionMs: Long, durationMs: Long): Boolean =
            durationMs > 0L && positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS

        fun isWatchedProgress(positionMs: Long, durationMs: Long): Boolean {
            if (!isMeaningfulProgress(positionMs, durationMs)) return false
            return if (durationMs <= WATCHED_REMAINING_MS) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) >=
                        SHORT_EPISODE_WATCHED_PROGRESS
            } else {
                positionMs >= durationMs - WATCHED_REMAINING_MS
            }
        }

        const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
        const val WATCHED_REMAINING_MS = 5 * 60 * 1_000L
        const val SHORT_EPISODE_WATCHED_PROGRESS = 0.90f
    }
}

internal data class ServerContinueProgress(
    val video: ContinueWatchingPlaybackVideo,
    val positionMs: Long,
    val updatedAt: Long,
)

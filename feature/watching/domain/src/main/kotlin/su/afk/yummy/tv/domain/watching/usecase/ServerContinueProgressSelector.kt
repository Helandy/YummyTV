package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.utils.episode.episodeNumberOrNull
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
    }
}

internal data class ServerContinueProgress(
    val video: ContinueWatchingPlaybackVideo,
    val positionMs: Long,
    val updatedAt: Long,
)

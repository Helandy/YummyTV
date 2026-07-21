package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.watching.mapper.bestUrl
import su.afk.yummy.tv.domain.watching.mapper.toContinueWatchingPlaybackVideo
import java.lang.Math.abs
import javax.inject.Inject

internal class ContinueWatchingLaunchResolver @Inject constructor(
    private val targetResolver: ContinueWatchingTargetResolver,
    private val serverProgressSelector: ServerContinueProgressSelector,
    private val progressMigrationFactory: ContinueWatchingProgressMigrationFactory,
) {

    fun resolve(
        entry: HomeContinueWatchingItem,
        availableVideos: List<AnimeVideo>,
        useServerProgress: Boolean,
    ): ContinueWatchingLaunchResolution {
        val localProgressVideo = entry.toContinueWatchingPlaybackVideo()
        val serverProgress = if (useServerProgress) {
            serverProgressSelector.select(availableVideos)
        } else {
            null
        }
        val posterUrl = entry.poster.bestUrl().orEmpty()

        if (serverProgress != null && serverProgress.updatedAt > entry.updatedAt) {
            return resolveServerProgress(
                entry = entry,
                serverProgress = serverProgress,
                localProgressVideo = localProgressVideo,
                availableVideos = availableVideos,
                posterUrl = posterUrl,
            )
        }

        val target = targetResolver.resolve(localProgressVideo, availableVideos)
        return ContinueWatchingLaunchResolution(
            launch = entry.toLaunch(
                posterUrl = posterUrl,
                video = target,
                resumeFromMs = entry.positionMs,
            ),
            progressMigration = progressMigrationFactory.create(
                entry = entry,
                progressVideo = localProgressVideo,
                targetVideo = target,
                posterUrl = posterUrl,
            ),
        )
    }

    private fun resolveServerProgress(
        entry: HomeContinueWatchingItem,
        serverProgress: ServerContinueProgress,
        localProgressVideo: ContinueWatchingPlaybackVideo,
        availableVideos: List<AnimeVideo>,
        posterUrl: String,
    ): ContinueWatchingLaunchResolution {
        val target = targetResolver.resolve(serverProgress.video, availableVideos)
        val localTarget = targetResolver.resolve(localProgressVideo, availableVideos)
        val remoteSwitch = if (
            target.isDifferentLaunchThan(
                other = localTarget,
                positionMs = serverProgress.positionMs,
                otherPositionMs = entry.positionMs,
            )
        ) {
            ContinueWatchingRemoteProgressSwitch(target.episode, serverProgress.positionMs)
        } else {
            null
        }
        return ContinueWatchingLaunchResolution(
            launch = entry.toLaunch(
                posterUrl = posterUrl,
                video = target,
                resumeFromMs = serverProgress.positionMs,
                remoteProgressSwitch = remoteSwitch,
            )
        )
    }

    private companion object {
        fun HomeContinueWatchingItem.toLaunch(
            posterUrl: String,
            video: ContinueWatchingPlaybackVideo,
            resumeFromMs: Long,
            remoteProgressSwitch: ContinueWatchingRemoteProgressSwitch? = null,
        ) = ContinueWatchingLaunch(
            animeId = animeId,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            video = video,
            resumeFromMs = resumeFromMs,
            remoteProgressSwitch = remoteProgressSwitch,
        )

        fun ContinueWatchingPlaybackVideo.isDifferentLaunchThan(
            other: ContinueWatchingPlaybackVideo,
            positionMs: Long,
            otherPositionMs: Long,
        ): Boolean =
            id.takeIf { it > 0 } != other.id.takeIf { it > 0 } ||
                    iframeUrl != other.iframeUrl ||
                    episode != other.episode ||
                    abs(positionMs - otherPositionMs) >
                    REMOTE_PROGRESS_TOAST_POSITION_EPSILON_MS

        const val REMOTE_PROGRESS_TOAST_POSITION_EPSILON_MS = 5_000L
    }
}

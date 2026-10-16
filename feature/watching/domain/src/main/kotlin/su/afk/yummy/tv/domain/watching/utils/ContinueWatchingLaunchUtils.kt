package su.afk.yummy.tv.domain.watching.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.player.isServerMirrorOfExtraSource
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.watching.ContinueWatchingLaunch
import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo
import su.afk.yummy.tv.core.model.watching.ContinueWatchingRemoteProgressSwitch
import su.afk.yummy.tv.domain.home.model.ContinueWatchingSource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.bestUrl
import su.afk.yummy.tv.domain.watching.model.ContinueWatchingLaunchResolution
import su.afk.yummy.tv.domain.watching.model.ServerContinueProgress
import kotlin.math.abs

private const val REMOTE_PROGRESS_TOAST_POSITION_EPSILON_MS = 10_000L

/** Выбирает источник и позицию запуска: локальный прогресс или более свежий серверный. */
internal fun resolveContinueWatchingLaunch(
    entry: HomeContinueWatchingItem,
    availableVideos: List<AnimeVideo>,
    useServerProgress: Boolean,
): ContinueWatchingLaunchResolution {
    val localProgressVideo = entry.toContinueWatchingPlaybackVideo()
    val serverProgress = if (useServerProgress) {
        availableVideos.selectServerContinueProgress()
    } else {
        null
    }
    val posterUrl = entry.poster.bestUrl().orEmpty()

    if (entry.source == ContinueWatchingSource.REMOTE) {
        return resolveRemoteEntryLaunch(entry, serverProgress, availableVideos, posterUrl)
    }

    if (serverProgress != null && serverProgress.updatedAt > entry.updatedAt &&
        !serverProgress.isMirrorOf(entry)
    ) {
        return resolveServerProgressLaunch(
            entry = entry,
            serverProgress = serverProgress,
            localProgressVideo = localProgressVideo,
            availableVideos = availableVideos,
            posterUrl = posterUrl,
        )
    }

    val target = resolveContinueWatchingTarget(localProgressVideo, availableVideos)
    return ContinueWatchingLaunchResolution(
        launch = entry.toLaunch(
            posterUrl = posterUrl,
            video = target,
            resumeFromMs = entry.positionMs,
        ),
        progressMigration = createContinueWatchingProgressMigration(
            entry = entry,
            progressVideo = localProgressVideo,
            targetVideo = target,
            posterUrl = posterUrl,
        ),
    )
}

/**
 * Карточка из серверной истории знает только номер серии и позицию. Видео с озвучкой берём из
 * `watched` в `/videos` — это тот же просмотр, если он не старше карточки; иначе ищем серию по номеру
 * («1» и «01» — одна серия). Тоста нет: локального прогресса, от которого переключаемся, тоже нет.
 */
private fun resolveRemoteEntryLaunch(
    entry: HomeContinueWatchingItem,
    serverProgress: ServerContinueProgress?,
    availableVideos: List<AnimeVideo>,
    posterUrl: String,
): ContinueWatchingLaunchResolution {
    if (serverProgress != null && serverProgress.updatedAt >= entry.updatedAt) {
        return ContinueWatchingLaunchResolution(
            launch = entry.toLaunch(
                posterUrl = posterUrl,
                video = resolveContinueWatchingTarget(serverProgress.video, availableVideos),
                resumeFromMs = serverProgress.positionMs,
            ),
        )
    }
    val episodeKey = entry.episode.episodeGroupKey()
    val episode = availableVideos
        .firstOrNull { it.episode.episodeGroupKey() == episodeKey }
        ?.episode
        ?: entry.episode
    val progressVideo = entry.toContinueWatchingPlaybackVideo().copy(episode = episode)
    return ContinueWatchingLaunchResolution(
        launch = entry.toLaunch(
            posterUrl = posterUrl,
            video = resolveContinueWatchingTarget(progressVideo, availableVideos),
            resumeFromMs = entry.positionMs,
        ),
    )
}

/**
 * Запуск следующей серии с начала: серия ищется по номеру («1» и «01» — одна серия), озвучка и плеер —
 * как у последнего просмотра тайтла на сервере (`watched` в `/videos`). null — такой серии нет.
 */
internal fun resolveNextEpisodeLaunch(
    entry: HomeContinueWatchingItem,
    availableVideos: List<AnimeVideo>,
): ContinueWatchingLaunch? {
    val episodeKey = entry.episode.episodeGroupKey()
    val episodeVideos = availableVideos.filter { it.episode.episodeGroupKey() == episodeKey }
    val episode = episodeVideos.firstOrNull()?.episode ?: return null
    val lastWatched = availableVideos
        .filter { (it.watchedDateSeconds ?: 0L) > 0L }
        .maxByOrNull { it.watchedDateSeconds ?: 0L }
    val progressVideo = entry.toContinueWatchingPlaybackVideo().copy(
        episode = episode,
        player = lastWatched?.player.orEmpty(),
        dubbing = lastWatched?.dubbing.orEmpty(),
    )
    // Подбираем только среди видео этой серии: без рабочего плеера подбор ушёл бы в другую.
    val target = resolveContinueWatchingTarget(progressVideo, episodeVideos)
    return entry.toLaunch(
        posterUrl = entry.poster.bestUrl().orEmpty(),
        video = target,
        resumeFromMs = 0L,
    )
}

/** Серверный прогресс — зеркало локального прогресса 4K той же серии (см. [isServerMirrorOfExtraSource]). */
private fun ServerContinueProgress.isMirrorOf(entry: HomeContinueWatchingItem): Boolean =
    video.episode.episodeGroupKey() == entry.episode.episodeGroupKey() &&
        isServerMirrorOfExtraSource(
            localEpisodeUrl = entry.episodeUrl,
            localDubbing = entry.dubbing,
            localPositionMs = entry.positionMs,
            serverEpisodeUrl = video.iframeUrl,
            serverDubbing = video.dubbing,
            serverPositionMs = positionMs,
        )

private fun resolveServerProgressLaunch(
    entry: HomeContinueWatchingItem,
    serverProgress: ServerContinueProgress,
    localProgressVideo: ContinueWatchingPlaybackVideo,
    availableVideos: List<AnimeVideo>,
    posterUrl: String,
): ContinueWatchingLaunchResolution {
    val target = resolveContinueWatchingTarget(serverProgress.video, availableVideos)
    val localTarget = resolveContinueWatchingTarget(localProgressVideo, availableVideos)
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
        ),
    )
}

private fun HomeContinueWatchingItem.toLaunch(
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

private fun ContinueWatchingPlaybackVideo.isDifferentLaunchThan(
    other: ContinueWatchingPlaybackVideo,
    positionMs: Long,
    otherPositionMs: Long,
): Boolean =
    id.takeIf { it > 0 } != other.id.takeIf { it > 0 } ||
        iframeUrl != other.iframeUrl ||
        episode != other.episode ||
        abs(positionMs - otherPositionMs) > REMOTE_PROGRESS_TOAST_POSITION_EPSILON_MS

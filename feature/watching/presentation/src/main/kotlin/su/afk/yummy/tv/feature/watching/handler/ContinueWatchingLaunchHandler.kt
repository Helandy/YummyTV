package su.afk.yummy.tv.feature.watching.handler

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.feature.player.ContinueWatchingTarget
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest
import su.afk.yummy.tv.feature.player.isTrustedPlaceholderMigrationTarget
import su.afk.yummy.tv.feature.player.resolveContinueWatchingTarget
import javax.inject.Inject

class ContinueWatchingLaunchHandler @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val playerNavigator: IPlayerNavigator,
) {
    suspend fun getPlayerLaunchResult(entry: HomeContinueWatchingItem): ContinueWatchingLaunchResult {
        val refreshProgressOnLaunch = settingsStore.refreshContinueWatchingProgressOnLaunch.first()
        val videos = if (entry.animeId != 0) {
            loadVideos(entry.animeId, refreshProgressOnLaunch)
        } else {
            emptyList()
        }

        val videoSources = videos.map { it.toPlayerVideoSource() }
        val progressVideo = entry.toPlayerVideoSource()
        val serverProgress = if (refreshProgressOnLaunch) {
            videos.bestServerContinueProgress()
        } else {
            null
        }
        if (serverProgress != null && serverProgress.updatedAt > entry.updatedAt) {
            val target = resolveContinueWatchingTarget(serverProgress.video, videoSources)
            val localTarget = resolveContinueWatchingTarget(progressVideo, videoSources)
            return ContinueWatchingLaunchResult(
                destination = playerNavigator.getPlayerDest(
                    video = target.video,
                    animeTitle = entry.animeTitle,
                    animeId = entry.animeId,
                    posterUrl = entry.poster.bestUrl().orEmpty(),
                    resumeFromMs = serverProgress.positionMs,
                ),
                remoteProgressSwitch = if (
                    target.isDifferentLaunchThan(
                        other = localTarget,
                        positionMs = serverProgress.positionMs,
                        otherPositionMs = entry.positionMs,
                    )
                ) {
                    ContinueWatchingRemoteProgressSwitch(
                        episode = target.video.episode,
                        positionMs = serverProgress.positionMs,
                    )
                } else {
                    null
                },
            )
        }

        val target = resolveContinueWatchingTarget(progressVideo, videoSources)
        migratePlaceholderEpisode(entry, progressVideo, target.video)

        return ContinueWatchingLaunchResult(
            destination = playerNavigator.getPlayerDest(
                video = target.video,
                animeTitle = entry.animeTitle,
                animeId = entry.animeId,
                posterUrl = entry.poster.bestUrl().orEmpty(),
                resumeFromMs = entry.positionMs,
            ),
        )
    }

    private suspend fun loadVideos(
        animeId: Int,
        refreshProgressOnLaunch: Boolean,
    ): List<AnimeVideo> =
        if (refreshProgressOnLaunch) {
            runCatching { refreshAnimeVideos(animeId) }
                .getOrElse {
                    runCatching { getAnimeVideos(animeId) }.getOrNull().orEmpty()
                }
        } else {
            runCatching { getAnimeVideos(animeId) }.getOrNull().orEmpty()
        }

    private suspend fun migratePlaceholderEpisode(
        entry: HomeContinueWatchingItem,
        progressVideo: PlayerVideoSource,
        targetVideo: PlayerVideoSource,
    ) {
        if (!progressVideo.isTrustedPlaceholderMigrationTarget(targetVideo)) return
        watchProgressStore.save(
            animeId = entry.animeId,
            episode = targetVideo.episode,
            videoId = targetVideo.id,
            episodeUrl = targetVideo.iframeUrl,
            positionMs = entry.positionMs,
            durationMs = entry.durationMs,
            animeTitle = entry.animeTitle,
            posterUrl = entry.poster.bestUrl().orEmpty(),
            playerName = targetVideo.player,
            dubbing = targetVideo.dubbing,
            screenshotUrl = entry.screenshotUrl,
        )
        watchProgressStore.delete(entry.animeId, entry.episode)
    }
}

data class ContinueWatchingLaunchResult(
    val destination: NavKey,
    val remoteProgressSwitch: ContinueWatchingRemoteProgressSwitch? = null,
)

data class ContinueWatchingRemoteProgressSwitch(
    val episode: String,
    val positionMs: Long,
)

private const val REMOTE_PROGRESS_TOAST_POSITION_EPSILON_MS = 5_000L

private fun ContinueWatchingTarget.isDifferentLaunchThan(
    other: ContinueWatchingTarget,
    positionMs: Long,
    otherPositionMs: Long,
): Boolean =
    video.id.takeIf { it > 0 } != other.video.id.takeIf { it > 0 } ||
            video.iframeUrl != other.video.iframeUrl ||
            video.episode != other.video.episode ||
            kotlin.math.abs(positionMs - otherPositionMs) > REMOTE_PROGRESS_TOAST_POSITION_EPSILON_MS

private data class ServerContinueProgress(
    val video: PlayerVideoSource,
    val positionMs: Long,
    val updatedAt: Long,
)

private fun List<AnimeVideo>.bestServerContinueProgress(): ServerContinueProgress? =
    mapNotNull { video ->
        val positionSeconds =
            video.watchedEndTimeSeconds?.takeIf { it >= 0 } ?: return@mapNotNull null
        val updatedAtSeconds =
            video.watchedDateSeconds?.takeIf { it > 0L } ?: return@mapNotNull null
        val durationSeconds = video.durationSeconds?.takeIf { it > 0 } ?: return@mapNotNull null
        val positionMs = positionSeconds * 1_000L
        val durationMs = durationSeconds * 1_000L
        if (!WatchProgressStore.isMeaningfulProgress(positionMs, durationMs)) return@mapNotNull null
        if (WatchProgressStore.isWatchedProgress(positionMs, durationMs)) return@mapNotNull null
        ServerContinueProgress(
            video = video.toPlayerVideoSource(),
            positionMs = positionMs,
            updatedAt = updatedAtSeconds * 1_000L,
        )
    }.maxWithOrNull(
        compareBy<ServerContinueProgress> { it.updatedAt }
            .thenBy { it.positionMs }
            .thenBy { it.video.episode.episodeNumberOrNull() ?: Double.NEGATIVE_INFINITY },
    )

private fun AnimeVideo.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    playerId = playerId,
    iframeUrl = iframeUrl,
    views = views,
    skips = skips.toPlayerSkips(),
)

private fun HomeContinueWatchingItem.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = videoId,
    episode = episode,
    dubbing = dubbing,
    player = playerName,
    iframeUrl = episodeUrl,
)

private fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

private fun AnimeVideoSkips.toPlayerSkips(): PlayerSkips = PlayerSkips(
    opening = opening.toPlayerSkipSegment(),
    ending = ending.toPlayerSkipSegment(),
)

private fun AnimeVideoSkipSegment?.toPlayerSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }

private fun String.episodeNumberOrNull(): Double? {
    val normalized = trim().replace(',', '.')
    return normalized.toDoubleOrNull()
        ?: Regex("""\d+(?:[.,]\d+)?""")
            .find(normalized)
            ?.value
            ?.replace(',', '.')
            ?.toDoubleOrNull()
}

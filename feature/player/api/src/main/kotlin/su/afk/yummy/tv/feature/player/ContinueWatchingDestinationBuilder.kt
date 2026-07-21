package su.afk.yummy.tv.feature.player

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.watching.usecase.ContinueWatchingLaunch
import su.afk.yummy.tv.domain.watching.usecase.ContinueWatchingPlaybackVideo

/** Maps a domain Continue Watching decision to the public player destination contract. */
fun IPlayerNavigator.getPlayerDest(launch: ContinueWatchingLaunch): NavKey =
    getPlayerDest(
        video = launch.video.toPlayerVideoSource(),
        animeTitle = launch.animeTitle,
        animeId = launch.animeId,
        posterUrl = launch.posterUrl,
        resumeFromMs = launch.resumeFromMs,
    )

private fun ContinueWatchingPlaybackVideo.toPlayerVideoSource() = PlayerVideoSource(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    playerId = playerId,
    iframeUrl = iframeUrl,
    views = views,
    skips = PlayerSkips(
        opening = skips.opening.toPlayerSkipSegment(),
        ending = skips.ending.toPlayerSkipSegment(),
    ),
)

private fun AnimeVideoSkipSegment?.toPlayerSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }

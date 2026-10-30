package su.afk.yummy.tv.data.player.mapper

import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import su.afk.yummy.tv.domain.player.model.PlayerSourceSkipSegment
import su.afk.yummy.tv.domain.player.model.PlayerSourceSkips
import su.afk.yummy.tv.domain.player.model.PlayerSourceVideo

internal fun AnimeVideo.toPlayerSourceVideo(): PlayerSourceVideo =
    PlayerSourceVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = playerId,
        iframeUrl = iframeUrl,
        views = views,
        skips = skips.toPlayerSourceSkips(),
        watchedEndTimeSeconds = watchedEndTimeSeconds,
        watchedDateSeconds = watchedDateSeconds,
        durationSeconds = durationSeconds,
    )

internal fun AnimeVideoSkips.toPlayerSourceSkips(): PlayerSourceSkips =
    PlayerSourceSkips(
        opening = opening.toPlayerSourceSkipSegment(),
        ending = ending.toPlayerSourceSkipSegment(),
    )

internal fun AnimeVideoSkipSegment?.toPlayerSourceSkipSegment(): PlayerSourceSkipSegment? =
    this?.let { PlayerSourceSkipSegment(startMs = it.startMs, endMs = it.endMs) }

internal fun List<AnimeScreenshot>.toScreenshotByEpisode(): Map<String, String> =
    mapNotNull { screenshot ->
        val episode = screenshot.episode ?: return@mapNotNull null
        val url = screenshot.url ?: return@mapNotNull null
        episode to url
    }.toMap()

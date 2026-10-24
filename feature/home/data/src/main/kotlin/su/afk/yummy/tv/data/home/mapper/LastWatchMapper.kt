package su.afk.yummy.tv.data.home.mapper

import su.afk.yummy.tv.core.common.episode.normalizedEpisodeNumber
import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.utils.network.toHttpsUrlOrNull
import su.afk.yummy.tv.data.home.dto.YaniLastWatchDto
import su.afk.yummy.tv.data.home.dto.YaniPosterDto
import su.afk.yummy.tv.domain.home.model.ContinueWatchingSource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

internal fun List<YaniLastWatchDto>.toHomeContinueWatchingItems(): List<HomeContinueWatchingItem> =
    filter { it.animeId > 0 && it.date > 0L }
        .map { it.toHomeContinueWatchingItem() }

internal fun YaniLastWatchDto.toHomeContinueWatchingItem(): HomeContinueWatchingItem =
    HomeContinueWatchingItem(
        animeId = animeId,
        animeTitle = title,
        description = description,
        poster = poster?.toHomePoster(),
        videoId = 0,
        episode = episode.normalizedEpisodeNumber(),
        episodeUrl = "",
        positionMs = endTime.coerceAtLeast(0) * 1_000L,
        durationMs = duration.coerceAtLeast(0) * 1_000L,
        updatedAt = date * 1_000L,
        playerName = "",
        dubbing = "",
        screenshotUrl = screenshot?.sizes?.let { it.small ?: it.full }.toHttpsUrlOrNull().orEmpty(),
        source = ContinueWatchingSource.REMOTE,
    )

private fun YaniPosterDto.toHomePoster(): AnimePoster =
    AnimePoster(
        small = small.toHttpsUrlOrNull(),
        medium = medium.toHttpsUrlOrNull(),
        big = big.toHttpsUrlOrNull(),
        fullsize = fullsize.toHttpsUrlOrNull(),
        mega = mega.toHttpsUrlOrNull(),
    )

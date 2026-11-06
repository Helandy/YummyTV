package su.afk.yummy.tv.data.library.mapper

import su.afk.yummy.tv.core.utils.network.toHttpsUrlOrNull
import su.afk.yummy.tv.data.library.dto.YaniWatchHistoryDto
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry

internal fun YaniWatchHistoryDto.toDomainOrNull(): WatchHistoryEntry? {
    if (animeId <= 0) return null
    return WatchHistoryEntry(
        animeId = animeId,
        animeUrl = animeUrl,
        title = title.ifBlank { animeUrl },
        episode = episode.ifBlank { screenshot?.episode.orEmpty() },
        posterUrl = poster?.mega.toHttpsUrlOrNull(),
        screenshotUrl = screenshot?.sizes?.run { full ?: small }.toHttpsUrlOrNull(),
        watchedAtSeconds = date,
        positionSeconds = endTime.coerceAtLeast(0),
        durationSeconds = duration.coerceAtLeast(0),
    )
}

package su.afk.yummy.tv.data.library.repository

import su.afk.yummy.tv.data.library.dto.YaniWatchHistoryDto
import su.afk.yummy.tv.data.library.network.YaniWatchHistoryApi
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.domain.library.repository.WatchHistoryRepository
import javax.inject.Inject

class YaniWatchHistoryRepository @Inject constructor(
    private val api: YaniWatchHistoryApi,
) : WatchHistoryRepository {
    override suspend fun getPage(limit: Int, offset: Int): List<WatchHistoryEntry> =
        api.getPage(limit, offset).response.mapNotNull { it.toDomainOrNull() }
}

private fun YaniWatchHistoryDto.toDomainOrNull(): WatchHistoryEntry? {
    if (animeId <= 0 || videoId <= 0) return null
    return WatchHistoryEntry(
        animeId = animeId,
        videoId = videoId,
        animeUrl = animeUrl,
        title = title.ifBlank { animeUrl },
        episode = episode ?: screenshot?.episode.orEmpty(),
        episodeTitle = episodeTitle,
        posterUrl = poster?.run { mega ?: huge ?: big ?: medium ?: small ?: fullsize }.toHttps(),
        screenshotUrl = screenshot?.sizes?.run { full ?: small }.toHttps(),
        watchedAtSeconds = date,
        positionSeconds = endTime.coerceAtLeast(0),
        durationSeconds = duration.coerceAtLeast(0),
        dubbing = dubbing,
        player = player,
    )
}

private fun String?.toHttps(): String? = this?.trim()?.takeIf { it.isNotEmpty() }?.let {
    when {
        it.startsWith("//") -> "https:$it"
        it.startsWith("http://") -> "https://${it.removePrefix("http://")}"
        else -> it
    }
}

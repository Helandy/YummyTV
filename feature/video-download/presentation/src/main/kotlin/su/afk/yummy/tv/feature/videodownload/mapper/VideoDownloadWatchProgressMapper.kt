package su.afk.yummy.tv.feature.videodownload.mapper

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.feature.videodownload.model.VideoDownloadWatchProgress

/**
 * Сопоставляет скачанные серии с локальным прогрессом просмотра по тайтлу и номеру серии
 * (`"1"` и `"01"` — одна серия). Результат ключуется id загрузки; серии без осмысленного прогресса
 * в карту не попадают.
 */
fun List<VideoDownloadItem>.toWatchProgressByDownload(
    progress: List<AnimeWatchProgress>,
): ImmutableMap<Long, VideoDownloadWatchProgress> {
    val byEpisode = progress
        .filter { it.animeId > 0 && it.isMeaningfulProgress() }
        .groupBy { it.animeId to it.episode.episodeGroupKey() }
        .mapValues { (_, entries) -> entries.maxBy { it.updatedAt } }
    return filter { it.status == VideoDownloadStatus.Downloaded }
        .mapNotNull { item ->
            val entry = byEpisode[item.animeId to item.episode.episodeGroupKey()] ?: return@mapNotNull null
            item.id to VideoDownloadWatchProgress(
                positionMs = entry.positionMs,
                durationMs = entry.durationMs,
                isWatched = entry.isWatchedProgress(),
            )
        }
        .toMap()
        .toImmutableMap()
}

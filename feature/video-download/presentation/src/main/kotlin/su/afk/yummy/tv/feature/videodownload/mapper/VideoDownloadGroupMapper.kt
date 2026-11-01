package su.afk.yummy.tv.feature.videodownload.mapper

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.feature.videodownload.model.VideoDownloadGroup
import su.afk.yummy.tv.feature.videodownload.utils.episodeSortComparator

/**
 * Собирает плоский список серий в сериалы: по `animeId` (без id — по названию),
 * свежие загрузки выше, серии внутри сериала — по возрастанию номера.
 */
fun List<VideoDownloadItem>.toGroups(): ImmutableList<VideoDownloadGroup> =
    groupBy { it.groupKey() }
        .map { (key, episodes) ->
            val first = episodes.first()
            VideoDownloadGroup(
                key = key,
                animeId = first.animeId,
                title = first.animeTitle,
                posterUrl = episodes.firstOrNull { it.posterUrl.isNotBlank() }?.posterUrl.orEmpty(),
                episodes = episodes.sortedWith(episodeSortComparator).toImmutableList(),
            )
        }
        .sortedByDescending { group -> group.episodes.maxOf { it.updatedAt } }
        .toImmutableList()

private fun VideoDownloadItem.groupKey(): String =
    if (animeId > 0) "anime:$animeId" else "title:$animeTitle"

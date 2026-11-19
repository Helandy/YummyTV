package su.afk.yummy.tv.feature.details.episodes.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.common.episode.normalizedEpisodeNumber
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.EpisodeGroup
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange

/** Размер одного диапазона серий и порог, выше которого показывается полоса быстрого перехода. */
const val EPISODE_RANGE_CHUNK_SIZE = 50

/**
 * Разбивает [groups] на диапазоны по [chunkSize] серий для быстрого перехода.
 *
 * Для обычных тайтлов (`groups.size <= chunkSize`) возвращает пустой список, чтобы UI оставался прежним.
 */
fun buildEpisodeRanges(
    groups: List<EpisodeGroup>,
    chunkSize: Int = EPISODE_RANGE_CHUNK_SIZE,
): ImmutableList<EpisodeRange> {
    require(chunkSize > 0) { "chunkSize must be positive, was $chunkSize" }
    if (groups.size <= chunkSize) return persistentListOf()
    return (groups.indices step chunkSize).map { startIndex ->
        val endIndex = (startIndex + chunkSize - 1).coerceAtMost(groups.lastIndex)
        EpisodeRange(
            startIndex = startIndex,
            endIndex = endIndex,
            firstEpisode = groups[startIndex].episode.normalizedEpisodeNumber(),
            lastEpisode = groups[endIndex].episode.normalizedEpisodeNumber(),
        )
    }.toImmutableList()
}

/** Возвращает индекс диапазона, содержащего [episodeIndex], либо `-1` для пустого списка. */
fun List<EpisodeRange>.rangeIndexForEpisodeIndex(episodeIndex: Int): Int {
    if (isEmpty()) return -1
    val clamped = episodeIndex.coerceIn(first().startIndex, last().endIndex)
    val index = indexOfFirst { clamped in it.startIndex..it.endIndex }
    return if (index >= 0) index else 0
}

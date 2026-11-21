package su.afk.yummy.tv.feature.details.episodes.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.core.common.episode.normalizedEpisodeNumber
import su.afk.yummy.tv.feature.details.episodes.EpisodesState.EpisodeGroup
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange
import su.afk.yummy.tv.feature.details.presentation.R

/**
 * Ширина диапазона в номерах серий (`1–50`, `51–100`, …) и порог числа серий, выше которого
 * показывается полоса быстрого перехода.
 */
const val EPISODE_RANGE_CHUNK_SIZE = 50

/** Блок для серий без числа в номере (например, `special`): уходят в отдельный хвостовой диапазон. */
private const val NON_NUMERIC_BLOCK = Int.MAX_VALUE

private val PlainEpisodeNumberRegex = Regex("""\d+(?:[.,]\d+)?""")

/**
 * Разбивает [groups] на диапазоны по номерам серий для быстрого перехода.
 *
 * Блок задаётся номером серии: `1..chunkSize`, `chunkSize+1..2*chunkSize` и т. д. Число записей в блоке
 * может быть любым — сдвоенные серии (`57-58`, `221+222`) попадают в блок по первому числу и не сдвигают
 * границы. Подпись диапазона строится по первой и последней «обычной» серии блока.
 *
 * Для обычных тайтлов (`groups.size <= chunkSize`) и когда все серии лежат в одном блоке возвращает пустой
 * список, чтобы UI оставался прежним.
 */
fun buildEpisodeRanges(
    groups: List<EpisodeGroup>,
    chunkSize: Int = EPISODE_RANGE_CHUNK_SIZE,
): ImmutableList<EpisodeRange> {
    require(chunkSize > 0) { "chunkSize must be positive, was $chunkSize" }
    if (groups.size <= chunkSize) return persistentListOf()
    val ranges = buildList {
        var startIndex = 0
        while (startIndex < groups.size) {
            val block = groups[startIndex].episodeBlock(chunkSize)
            var endIndex = startIndex
            while (endIndex + 1 < groups.size && groups[endIndex + 1].episodeBlock(chunkSize) == block) {
                endIndex++
            }
            add(groups.toEpisodeRange(startIndex, endIndex))
            startIndex = endIndex + 1
        }
    }
    return if (ranges.size < 2) persistentListOf() else ranges.toImmutableList()
}

/** Номер блока серии: 0 для серий до [chunkSize] включительно (и для номеров ≤ 0), далее по возрастанию. */
private fun EpisodeGroup.episodeBlock(chunkSize: Int): Int {
    val number = episode.episodeNumberOrNull() ?: return NON_NUMERIC_BLOCK
    val whole = number.toInt()
    return if (whole <= 0) 0 else (whole - 1) / chunkSize
}

private fun List<EpisodeGroup>.toEpisodeRange(startIndex: Int, endIndex: Int): EpisodeRange {
    val chunk = subList(startIndex, endIndex + 1)
    val first = chunk.firstOrNull { it.episode.isPlainEpisodeNumber() } ?: chunk.first()
    val last = chunk.lastOrNull { it.episode.isPlainEpisodeNumber() } ?: chunk.last()
    return EpisodeRange(
        startIndex = startIndex,
        endIndex = endIndex,
        firstEpisode = first.episode.normalizedEpisodeNumber(),
        lastEpisode = last.episode.normalizedEpisodeNumber(),
    )
}

/** Номер серии вида `12` или `12.5`, без сдвоенных (`57-58`, `221+222`) и текстовых (`OVA 1`). */
private fun String.isPlainEpisodeNumber(): Boolean = PlainEpisodeNumberRegex.matches(trim())

/** Возвращает индекс диапазона, содержащего [episodeIndex], либо `-1` для пустого списка. */
fun List<EpisodeRange>.rangeIndexForEpisodeIndex(episodeIndex: Int): Int {
    if (isEmpty()) return -1
    val clamped = episodeIndex.coerceIn(first().startIndex, last().endIndex)
    val index = indexOfFirst { clamped in it.startIndex..it.endIndex }
    return if (index >= 0) index else 0
}

/**
 * Определяет активный чип диапазона для прокручиваемого списка серий.
 *
 * - Если пользователь явно нажал чип ([pinnedRangeIndex]), он остаётся активным до начала ручного скролла
 *   (важно для короткого хвоста и высоких экранов, где `scrollToItem(range.startIndex)` упирается в конец).
 * - Если список докручен до упора вниз (`!canScrollForward && canScrollBackward`), подсвечивается последний
 *   диапазон, даже когда `firstVisibleItemIndex` всё ещё принадлежит предыдущему чанку (например, 101 серия).
 */
fun List<EpisodeRange>.resolveVisibleRangeIndex(
    firstVisibleItemIndex: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean = firstVisibleItemIndex > 0,
    pinnedRangeIndex: Int? = null,
): Int {
    if (isEmpty()) return -1
    if (pinnedRangeIndex != null && pinnedRangeIndex in indices) {
        return pinnedRangeIndex
    }
    if (!canScrollForward && canScrollBackward) {
        return lastIndex
    }
    return rangeIndexForEpisodeIndex(firstVisibleItemIndex)
}

/** Подпись чипа диапазона серий (`1–50`, либо одиночный номер на хвосте списка). */
@Composable
fun EpisodeRange.chipLabel(): String =
    if (firstEpisode == lastEpisode) {
        firstEpisode
    } else {
        stringResource(R.string.details_episodes_range_label, firstEpisode, lastEpisode)
    }

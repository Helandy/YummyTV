package su.afk.yummy.tv.domain.home.utils

import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress

/**
 * Целый номер серии или `null`. Спецвыпуски вроде «7.5» — не серия 7, иначе она ложно считалась
 * бы просмотренной.
 */
fun String.wholeEpisodeNumberOrNull(): Int? =
    episodeNumberOrNull()?.takeIf { it % 1.0 == 0.0 }?.toInt()

/** Досмотренные серии из прогресса: id тайтла → целые номера серий. */
fun List<AnimeWatchProgress>.watchedEpisodeNumbers(): Map<Int, Set<Int>> =
    asSequence()
        .filter { it.animeId > 0 && it.isWatchedProgress() }
        .groupBy({ it.animeId }, { it.episode.wholeEpisodeNumberOrNull() })
        .mapValues { (_, numbers) -> numbers.filterNotNullTo(mutableSetOf()) }
        .filterValues { it.isNotEmpty() }

/** Объединяет две карты просмотренных серий по тайтлам. */
fun Map<Int, Set<Int>>.unionWatched(other: Map<Int, Set<Int>>): Map<Int, Set<Int>> {
    if (other.isEmpty()) return this
    if (isEmpty()) return other
    return (keys + other.keys).associateWith { id -> this[id].orEmpty() + other[id].orEmpty() }
}

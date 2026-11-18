package su.afk.yummy.tv.data.library.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry

/**
 * Считает число уникальных досмотренных основных серий по `animeId` из локальной таблицы
 * `watch_progress`. Дробные спецвыпуски (`7.5`) не учитываются, чтобы не завышать счётчик серий.
 */
internal fun List<WatchProgressEntry>.countWatchedEpisodesByAnimeId(): Map<Int, Int> {
    val keysByAnimeId = linkedMapOf<Int, MutableSet<String>>()
    forEach { entry ->
        if (entry.animeId <= 0 || entry.episode.isBlank()) return@forEach
        if (!isWatchedProgress(entry.positionMs, entry.durationMs)) return@forEach
        val number = entry.episode.episodeNumberOrNull()
        if (number != null && (number <= 0.0 || number % 1.0 != 0.0)) return@forEach
        val key = if (number != null) {
            number.toInt().toString()
        } else {
            entry.episode.episodeGroupKey()
        }
        keysByAnimeId.getOrPut(entry.animeId) { linkedSetOf() } += key
    }
    return keysByAnimeId.mapValues { (_, episodes) -> episodes.size }
}

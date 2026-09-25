package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry

/** Ключ локального прогресса серии в истории: серии с ведущим нулём ("01" и "1") совпадают. */
val WatchHistoryEntry.historyProgressKey: String
    get() = progressKey(animeId, episode)

internal val AnimeWatchProgress.historyProgressKey: String
    get() = progressKey(animeId, episode)

private fun progressKey(animeId: Int, episode: String): String = "$animeId:${episode.episodeGroupKey()}"

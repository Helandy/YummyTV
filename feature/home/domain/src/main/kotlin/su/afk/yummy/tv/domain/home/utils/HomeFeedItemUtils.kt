package su.afk.yummy.tv.domain.home.utils

import su.afk.yummy.tv.domain.home.model.HomeFeedItem

/** Отмечена ли просмотренной именно та серия, которую показывает карточка. */
fun HomeFeedItem.isEpisodeWatched(watchedEpisodes: Map<Int, Set<Int>>): Boolean =
    episodeNumber != null && episodeNumber in watchedEpisodes[id].orEmpty()

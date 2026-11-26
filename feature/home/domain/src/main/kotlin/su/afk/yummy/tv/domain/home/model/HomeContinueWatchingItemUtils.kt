package su.afk.yummy.tv.domain.home.model

fun HomeContinueWatchingItem.hasPlayableTarget(): Boolean =
    videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

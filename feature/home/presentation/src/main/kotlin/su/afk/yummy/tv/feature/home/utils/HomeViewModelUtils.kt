package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

internal fun HomeContinueWatchingItem.hasPlayableTarget(): Boolean =
    videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

internal fun Long.toToastTimeString(): String {
    val totalSeconds = coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

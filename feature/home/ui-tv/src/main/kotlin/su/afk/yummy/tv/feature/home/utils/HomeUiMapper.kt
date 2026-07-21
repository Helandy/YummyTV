package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomePoster

internal fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

internal fun HomeFeedItem.focusKey(): String = when (val action = action) {
    is HomeFeedItemAction.OpenSeries -> "series:${action.seriesId}"
    is HomeFeedItemAction.OpenCollection -> "collection:${action.collectionId}"
    is HomeFeedItemAction.OpenVideo -> "video:${action.videoId}"
}

internal fun HomeFeedItem.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> poster?.medium ?: poster?.big ?: poster?.fullsize ?: poster?.small
    PosterQuality.STANDARD -> poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small
    PosterQuality.MEGA -> poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.fullsize
    ?: poster?.small

    PosterQuality.HIGH -> poster?.fullsize ?: poster?.mega ?: poster?.big ?: poster?.medium
    ?: poster?.small
}

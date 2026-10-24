package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

internal fun HomeFeedItem.focusKey(): String = when (val action = action) {
    is HomeFeedItemAction.OpenSeries -> "series:${action.seriesId}"
    is HomeFeedItemAction.OpenCollection -> "collection:${action.collectionId}"
    is HomeFeedItemAction.OpenVideo -> "video:${action.videoId}"
}

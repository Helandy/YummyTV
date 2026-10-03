package su.afk.yummy.tv.feature.home.mapper

import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.HomeState

/** Видео пока не имеет собственного экрана — событие не отправляется. */
fun HomeFeedItemAction.toHomeEventOrNull(): HomeState.Event? = when (this) {
    is HomeFeedItemAction.OpenSeries -> HomeState.Event.AnimeSelected(seriesId)
    is HomeFeedItemAction.OpenCollection -> HomeState.Event.CollectionSelected(collectionId)
    is HomeFeedItemAction.OpenVideo -> null
}

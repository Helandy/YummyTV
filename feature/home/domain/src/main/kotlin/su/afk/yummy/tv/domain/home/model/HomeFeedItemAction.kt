package su.afk.yummy.tv.domain.home.model

sealed interface HomeFeedItemAction {
    data class OpenSeries(val seriesId: Int) : HomeFeedItemAction
    data class OpenVideo(val videoId: Int) : HomeFeedItemAction
    data class OpenCollection(val collectionId: Int) : HomeFeedItemAction
}

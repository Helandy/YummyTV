package su.afk.yummy.tv.domain.home

data class HomeFeed(
    val heroItems: List<HomeFeedItem>,
    val sections: List<HomeFeedSection>,
)

data class HomeFeedSection(
    val title: String,
    val items: List<HomeFeedItem>,
)

data class HomeFeedItem(
    val id: Int,
    val title: String,
    val description: String,
    val poster: HomePoster?,
    val rating: Double?,
    val action: HomeFeedItemAction,
)

data class HomePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

sealed interface HomeFeedItemAction {
    data class OpenSeries(val seriesId: Int) : HomeFeedItemAction
    data class OpenVideo(val videoId: Int) : HomeFeedItemAction
    data class OpenCollection(val collectionId: Int) : HomeFeedItemAction
}

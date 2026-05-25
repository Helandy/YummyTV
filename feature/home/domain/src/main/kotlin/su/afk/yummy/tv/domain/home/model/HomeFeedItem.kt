package su.afk.yummy.tv.domain.home

data class HomeFeedItem(
    val id: Int,
    val title: String,
    val description: String,
    val poster: HomePoster?,
    val rating: Double?,
    val action: HomeFeedItemAction,
)

package su.afk.yummy.tv.domain.home.model

data class HomeFeedItem(
    val id: Int,
    val title: String,
    val description: String,
    val poster: HomePoster?,
    val rating: Double?,
    val year: Int?,
    val action: HomeFeedItemAction,
)

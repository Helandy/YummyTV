package su.afk.yummy.tv.domain.collection

data class CollectionDetail(
    val id: Int,
    val title: String,
    val description: String,
    val views: Int,
    val posterUrl: String?,
    val animes: List<CollectionAnimeItem>,
)

data class CollectionAnimeItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
)

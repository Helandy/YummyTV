package su.afk.yummy.tv.domain.collection.model

data class CollectionAnimeItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
)

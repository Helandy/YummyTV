package su.afk.yummy.tv.domain.catalog.model

data class SearchItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
)

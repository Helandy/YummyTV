package su.afk.yummy.tv.domain.search.model

data class SearchItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
)

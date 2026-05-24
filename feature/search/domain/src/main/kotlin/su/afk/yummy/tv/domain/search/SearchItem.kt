package su.afk.yummy.tv.domain.search

data class SearchItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val blockedIn: List<String>,
)

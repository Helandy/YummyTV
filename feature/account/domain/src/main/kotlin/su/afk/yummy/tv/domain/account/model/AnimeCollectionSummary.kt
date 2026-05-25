package su.afk.yummy.tv.domain.account

data class AnimeCollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val views: Int?,
)

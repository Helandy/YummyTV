package su.afk.yummy.tv.domain.collection.model

data class CollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
)

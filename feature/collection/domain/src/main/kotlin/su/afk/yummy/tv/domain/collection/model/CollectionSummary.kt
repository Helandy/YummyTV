package su.afk.yummy.tv.domain.collection.model

data class CollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val likesCount: Int,
    val viewsCount: Int = 0,
    /** До трёх первых постеров коллекции — для коллажа в каталоге. */
    val posterUrls: List<String> = emptyList(),
)

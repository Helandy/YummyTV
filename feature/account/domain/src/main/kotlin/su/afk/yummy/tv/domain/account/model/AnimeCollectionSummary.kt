package su.afk.yummy.tv.domain.account.model

data class AnimeCollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val poster: AnimeCollectionPoster?,
    val views: Int?,
    val likes: Int? = null,
    /** До трёх первых постеров коллекции — для коллажа в карточке. */
    val posterUrls: List<String> = emptyList(),
)

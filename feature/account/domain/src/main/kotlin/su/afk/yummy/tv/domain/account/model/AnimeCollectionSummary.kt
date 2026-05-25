package su.afk.yummy.tv.domain.account.model

data class AnimeCollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val poster: AnimeCollectionPoster?,
    val views: Int?,
)

data class AnimeCollectionPoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

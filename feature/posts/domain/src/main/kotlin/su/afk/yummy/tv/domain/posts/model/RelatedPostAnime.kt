package su.afk.yummy.tv.domain.posts.model

data class RelatedPostAnime(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val year: Int?,
    val rating: Double?,
)

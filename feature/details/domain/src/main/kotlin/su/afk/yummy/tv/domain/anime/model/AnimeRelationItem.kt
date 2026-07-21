package su.afk.yummy.tv.domain.anime.model

data class AnimeRelationItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
)

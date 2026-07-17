package su.afk.yummy.tv.core.model.anime

data class AnimeViewingOrderItem(
    val animeId: Int,
    val title: String,
    val relation: String?,
    val type: String?,
    val episodesCount: Int?,
    val poster: AnimePoster?,
    val year: Int?,
    val rating: Double?,
)

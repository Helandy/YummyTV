package su.afk.yummy.tv.domain.anime.model

data class AnimeRecommendation(
    val animeId: Int,
    val title: String,
    val poster: AnimePoster?,
    val rating: Double?,
    val type: String?,
    val year: Int?,
)

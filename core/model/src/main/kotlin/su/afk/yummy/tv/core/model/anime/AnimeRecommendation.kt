package su.afk.yummy.tv.core.model.anime

data class AnimeRecommendation(
    val animeId: Int,
    val title: String,
    val poster: AnimePoster?,
    val rating: Double?,
    val type: String?,
    val year: Int?,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val vote: AnimeRecommendationVote = AnimeRecommendationVote.NONE,
)

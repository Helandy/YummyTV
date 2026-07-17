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

enum class AnimeRecommendationVote(val apiValue: Int) {
    DISLIKE(-1),
    NONE(0),
    LIKE(1),
    ;

    companion object {
        fun fromApi(value: Int): AnimeRecommendationVote =
            entries.firstOrNull { it.apiValue == value } ?: NONE
    }
}

data class AnimeRecommendationReaction(
    val likes: Int,
    val dislikes: Int,
    val vote: AnimeRecommendationVote,
)

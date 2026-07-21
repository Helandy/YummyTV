package su.afk.yummy.tv.core.model.anime

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

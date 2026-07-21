package su.afk.yummy.tv.core.model.anime

data class AnimeRecommendationReaction(
    val likes: Int,
    val dislikes: Int,
    val vote: AnimeRecommendationVote,
)

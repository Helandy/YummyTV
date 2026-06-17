package su.afk.yummy.tv.domain.collection.model

enum class CollectionVote(val apiValue: Int) {
    LIKE(1),
    NEUTRAL(0),
    DISLIKE(-1);

    companion object {
        fun fromApi(value: Int?): CollectionVote = when (value) {
            1 -> LIKE
            -1 -> DISLIKE
            else -> NEUTRAL
        }
    }
}

data class CollectionVoteResult(
    val likes: Int,
    val dislikes: Int,
)

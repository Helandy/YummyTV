package su.afk.yummy.tv.domain.comments.model

enum class CommentVote(val apiValue: Int) {
    LIKE(1),
    NEUTRAL(0),
    DISLIKE(-1);

    companion object {
        fun fromApi(value: Int?): CommentVote = when (value) {
            1 -> LIKE
            -1 -> DISLIKE
            else -> NEUTRAL
        }
    }
}

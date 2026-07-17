package su.afk.yummy.tv.domain.comments.model

data class Comment(
    val id: Int,
    val author: CommentAuthor,
    val text: String,
    val createdAtEpochSeconds: Long,
    val parentId: Int?,
    val childrenCount: Int,
    val likes: Int,
    val dislikes: Int,
    val vote: CommentVote,
    val roles: List<String>,
    val deletedAtEpochSeconds: Long?,
)

data class CommentAuthor(
    val id: Int,
    val name: String,
    val avatarSmallUrl: String?,
    val avatarBigUrl: String?,
    val avatarFullUrl: String?,
)

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

enum class CommentSort(val apiValue: String) {
    NEW("new"),
    OLD("old"),
    BEST("nice"),
}

data class CommentDraft(
    val text: String,
    val parentCommentId: Int? = null,
    val replyToCommentId: Int? = null,
)

enum class CommentReportReason(val apiValue: Int) {
    SPAM(1),
    INSULT(2),
    SPOILER(3),
    FLOOD(4),
    OFFTOPIC(5),
    OTHER(8),
}

data class CommentsPage(
    val comments: List<Comment>,
    val isModerator: Boolean,
)

data class CommentVoteResult(
    val likes: Int,
    val dislikes: Int,
    val success: Boolean,
)

enum class CommentTargetType(val apiValue: String) {
    ANIME("anime"),
    POST("post"),
    REVIEW("review"),
    USER("user"),
    BLOG_VIDEO("blogvideo"),
    COLLECTION("collection"),
}

data class CommentTarget(
    val type: CommentTargetType,
    val id: Int,
)

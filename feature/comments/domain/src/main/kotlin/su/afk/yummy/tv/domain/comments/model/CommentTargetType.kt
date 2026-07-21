package su.afk.yummy.tv.domain.comments.model

enum class CommentTargetType(val apiValue: String) {
    ANIME("anime"),
    POST("post"),
    REVIEW("review"),
    USER("user"),
    BLOG_VIDEO("blogvideo"),
    COLLECTION("collection"),
}

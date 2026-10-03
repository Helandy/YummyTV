package su.afk.yummy.tv.core.model.comments

enum class CommentTargetType(val apiValue: String) {
    ANIME("anime"),
    POST("post"),
    REVIEW("review"),
    USER("user"),
    BLOG_VIDEO("blogvideo"),
    COLLECTION("collection"),
}

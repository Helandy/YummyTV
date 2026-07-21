package su.afk.yummy.tv.domain.bloggers.model

enum class BloggerVideoVote(val apiValue: String?) {
    NONE(null), LIKE("like"), DISLIKE("dislike")
}

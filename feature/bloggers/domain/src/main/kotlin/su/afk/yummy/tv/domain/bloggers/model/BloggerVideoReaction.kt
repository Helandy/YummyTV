package su.afk.yummy.tv.domain.bloggers.model

data class BloggerVideoReaction(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val vote: BloggerVideoVote = BloggerVideoVote.NONE,
)

package su.afk.yummy.tv.domain.bloggers.model

data class BloggerDirectory(
    val categories: List<BloggerVideoCategory>,
    val bloggers: List<Blogger>,
)

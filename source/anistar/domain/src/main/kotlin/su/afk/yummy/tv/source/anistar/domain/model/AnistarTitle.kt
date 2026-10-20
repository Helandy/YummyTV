package su.afk.yummy.tv.source.anistar.domain.model

/** Тайтл, который сервер нашёл на AniStar: id новости на сайте и живое зеркало, на котором лежит плеер. */
data class AnistarTitle(
    val newsId: Long,
    val mirror: String,
)

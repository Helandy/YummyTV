package su.afk.yummy.tv.source.anistar.domain.model

/**
 * Тайтл, который сервер нашёл на AniStar: id новости на сайте, живое зеркало, на котором лежит плеер, и номера вышедших серий
 * (плеер-страницу читает сервер, клиент при открытии деталей на сайт не ходит).
 */
data class AnistarTitle(
    val newsId: Long,
    val mirror: String,
    val episodes: List<AnistarEpisode>,
)

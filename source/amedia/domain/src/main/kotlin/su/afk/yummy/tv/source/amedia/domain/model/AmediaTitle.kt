package su.afk.yummy.tv.source.amedia.domain.model

/** Страница тайтла на amd.online, которую нашёл сервер, и вышедшие серии с неё (сервер читает страницу сам). */
data class AmediaTitle(
    val pageUrl: String,
    val episodes: List<AmediaEpisode>,
)

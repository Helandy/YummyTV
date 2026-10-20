package su.afk.yummy.tv.source.anistar.domain.model

/** Адрес серии в источнике: id новости на сайте и номер серии в его плейлисте. */
data class AnistarTarget(
    val newsId: Long,
    val episode: Int,
)

package su.afk.yummy.tv.source.anidub.domain.model

/** Адрес серии в источнике: slug страницы тайтла на сайте и номер серии. */
data class AniDubTarget(
    val slug: String,
    val episode: Int,
)

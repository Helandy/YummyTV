package su.afk.yummy.tv.source.sameband.domain.model

/** Адрес серии в источнике: slug страницы тайтла на сайте и номер серии. */
data class SamebandTarget(
    val slug: String,
    val episode: Int,
)

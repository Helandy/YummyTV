package su.afk.yummy.tv.source.studiomir.domain.model

/** Адрес серии в источнике: slug страницы тайтла на сайте и номер серии. */
data class StudioMirTarget(
    val slug: String,
    val episode: Int,
)

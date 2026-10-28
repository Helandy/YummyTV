package su.afk.yummy.tv.source.animevost.domain.model

/** Адрес серии в источнике: раздел и slug страницы тайтла (`tv`, `3640-chitose-kun-…`) и номер серии. */
data class AnimevostTarget(
    val typePath: String,
    val slug: String,
    val episode: Int,
)

package su.afk.yummy.tv.source.ailiberty.domain.model

/** Адрес серии в источнике: slug релиза на сайте и номер серии. */
data class AiLibertyTarget(
    val slug: String,
    val episode: Int,
)

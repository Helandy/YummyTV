package su.afk.yummy.tv.source.dreamerscast.domain.model

/** Адрес серии в источнике: slug страницы релиза (`618-tensei-goblin-dakedo-shitsumon-aru`) и номер серии. */
data class DreamerscastTarget(
    val slug: String,
    val episode: Int,
)

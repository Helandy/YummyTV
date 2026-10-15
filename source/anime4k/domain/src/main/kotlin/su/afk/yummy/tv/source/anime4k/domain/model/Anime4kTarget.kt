package su.afk.yummy.tv.source.anime4k.domain.model

/** Что именно открывает пользователь: тайтл, серия и озвучка. */
data class Anime4kTarget(
    val malId: Int,
    val season: String,
    val episode: String,
    val team: String,
)

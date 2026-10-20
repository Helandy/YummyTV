package su.afk.yummy.tv.source.anistar.domain.model

/** Что приложение знает о тайтле: по этим данным сервер ищет пару на AniStar (внешних id там нет). */
data class AnistarLookup(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int?,
    val type: String?,
    val episodes: Int?,
)

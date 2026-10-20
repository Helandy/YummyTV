package su.afk.yummy.tv.source.aniboom.domain.model

/** Что приложение знает о тайтле: по этим данным сервер ищет пару в каталоге animego (внешних id там нет). */
data class AniboomLookup(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int?,
    val type: String?,
    val episodes: Int?,
)

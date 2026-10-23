package su.afk.yummy.tv.source.amedia.domain.model

/** Что приложение знает о тайтле: по этим данным сервер ищет страницу на amd.online (по MAL id или названиям, году, типу). */
data class AmediaLookup(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int?,
    val type: String?,
    val episodes: Int?,
)

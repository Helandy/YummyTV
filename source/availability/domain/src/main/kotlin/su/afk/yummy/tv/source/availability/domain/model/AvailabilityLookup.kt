package su.afk.yummy.tv.source.availability.domain.model

/** Что приложение знает о тайтле: по этим данным сервер находит его на всех балансерах одним запросом. */
data class AvailabilityLookup(
    val malId: Int?,
    val title: String,
    val otherTitles: List<String>,
    val year: Int?,
    val type: String?,
    val episodes: Int?,
)

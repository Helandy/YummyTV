package su.afk.yummy.tv.source.amedia.domain.model

/** Что приложение знает о тайтле: по названиям ищется страница на amd.online, MAL id подтверждает совпадение. */
data class AmediaLookup(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
)

package su.afk.yummy.tv.source.anime4k.domain.model

data class Anime4kLookup(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int?,
    val type: String?,
    val episodes: Int?,
)

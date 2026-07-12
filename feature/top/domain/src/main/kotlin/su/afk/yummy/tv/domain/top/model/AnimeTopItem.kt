package su.afk.yummy.tv.domain.top.model

data class AnimeTopItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
)

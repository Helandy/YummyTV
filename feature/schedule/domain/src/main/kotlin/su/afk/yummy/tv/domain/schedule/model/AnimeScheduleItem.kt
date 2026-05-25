package su.afk.yummy.tv.domain.schedule

data class AnimeScheduleItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val nextDateEpochSeconds: Long?,
    val airedEpisodes: Int?,
    val totalEpisodes: Int?,
    val previousDateEpochSeconds: Long? = null,
)

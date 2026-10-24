package su.afk.yummy.tv.domain.schedule.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

data class AnimeScheduleItem(
    val animeId: Int,
    val title: String,
    val poster: AnimePoster?,
    val nextDateEpochSeconds: Long?,
    val airedEpisodes: Int?,
    val totalEpisodes: Int?,
    val previousDateEpochSeconds: Long? = null,
)

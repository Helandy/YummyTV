package su.afk.yummy.tv.domain.anime.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

data class AnimeRelationItem(
    val animeId: Int,
    val title: String,
    val poster: AnimePoster?,
    val rating: Double?,
    val year: Int?,
)

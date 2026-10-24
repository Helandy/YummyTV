package su.afk.yummy.tv.domain.catalog.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

data class SearchItem(
    val id: Int,
    val title: String,
    val description: String = "",
    val poster: AnimePoster?,
    val rating: Double?,
    val year: Int?,
)

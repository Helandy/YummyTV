package su.afk.yummy.tv.domain.account.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

data class AnimeCollectionSummary(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val poster: AnimePoster?,
    val views: Int?,
    val likes: Int? = null,
    /** До трёх первых постеров коллекции — для коллажа в карточке. */
    val posterUrls: List<String> = emptyList(),
)

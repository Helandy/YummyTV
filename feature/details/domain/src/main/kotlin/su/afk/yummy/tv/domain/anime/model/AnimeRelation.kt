package su.afk.yummy.tv.domain.anime.model

data class AnimeRelation(
    val title: String,
    val secondaryTitle: String? = null,
    val description: String? = null,
    val subGenres: List<AnimeRelationSubGenre> = emptyList(),
    val anime: List<AnimeRelationItem>,
)

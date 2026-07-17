package su.afk.yummy.tv.domain.anime.model

enum class AnimeRelationKind { STUDIO, DIRECTOR, GENRE }

data class AnimeRelationReference(
    val kind: AnimeRelationKind,
    val id: Int,
    val url: String? = null,
)

data class AnimeRelation(
    val title: String,
    val secondaryTitle: String? = null,
    val description: String? = null,
    val subGenres: List<AnimeRelationSubGenre> = emptyList(),
    val anime: List<AnimeRelationItem>,
)

data class AnimeRelationSubGenre(
    val id: Int,
    val title: String,
    val alias: String? = null,
)

data class AnimeRelationItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
)

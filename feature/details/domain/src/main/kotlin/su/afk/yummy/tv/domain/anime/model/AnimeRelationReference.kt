package su.afk.yummy.tv.domain.anime.model

data class AnimeRelationReference(
    val kind: AnimeRelationKind,
    val id: Int,
    val url: String? = null,
)

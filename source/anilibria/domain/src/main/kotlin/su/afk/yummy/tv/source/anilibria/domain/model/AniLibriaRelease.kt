package su.afk.yummy.tv.source.anilibria.domain.model

/** Релиз AniLibria, сопоставленный с тайтлом по id MyAnimeList. */
data class AniLibriaRelease(
    val id: Int,
    val malId: Int,
    val episodes: List<AniLibriaEpisode>,
)

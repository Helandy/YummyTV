package su.afk.yummy.tv.source.anilibria.domain.model

/**
 * Релиз AniLibria, сопоставленный с тайтлом по id MyAnimeList. [isDubbing] — отдельный релиз «нового дубляжа»:
 * у одного тайтла он существует рядом с релизом озвучки.
 */
data class AniLibriaRelease(
    val id: Int,
    val malId: Int,
    val episodes: List<AniLibriaEpisode>,
    val isDubbing: Boolean = false,
)

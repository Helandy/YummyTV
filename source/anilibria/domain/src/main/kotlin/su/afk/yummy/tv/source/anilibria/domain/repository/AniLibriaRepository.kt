package su.afk.yummy.tv.source.anilibria.domain.repository

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease

interface AniLibriaRepository {
    /**
     * Ищет релизы по [titles] и выбирает те, у которых id MyAnimeList равен [malId] (у тайтла может быть релиз озвучки
     * и релиз нового дубляжа). Если MAL id нигде не заполнен, берёт единственный релиз с тем же названием и годом
     * ([year] ±1); пустой список — релизов нет или они заблокированы.
     */
    suspend fun findReleases(malId: Int, titles: List<String>, year: Int?): List<AniLibriaRelease>

    /** Релиз по id со списком серий; `null`, если его нет. */
    suspend fun getRelease(releaseId: Int): AniLibriaRelease?
}

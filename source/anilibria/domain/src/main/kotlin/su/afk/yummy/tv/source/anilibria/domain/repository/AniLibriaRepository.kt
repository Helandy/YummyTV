package su.afk.yummy.tv.source.anilibria.domain.repository

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease

interface AniLibriaRepository {
    /**
     * Ищет релиз по [titles] и выбирает тот, у которого id MyAnimeList равен [malId]. Если у подходящего релиза MAL id не заполнен,
     * берёт единственный с тем же названием и годом ([year] ±1); `null` — релиза нет или он заблокирован.
     */
    suspend fun findRelease(malId: Int, titles: List<String>, year: Int?): AniLibriaRelease?

    /** Релиз по id со списком серий; `null`, если его нет. */
    suspend fun getRelease(releaseId: Int): AniLibriaRelease?
}

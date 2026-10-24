package su.afk.yummy.tv.source.anilibria.domain.repository

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease

interface AniLibriaRepository {
    /**
     * Релизы тайтла из копии каталога на сервере YummyTV: по MAL id (у тайтла может быть релиз озвучки и релиз нового дубляжа),
     * а у релизов без MAL id — по названию и году. Серии приходят с сервера без ссылок на потоки ([AniLibriaEpisode.hls480] и
     * остальные `null`), к AniLibria это не обращается. Пустой список — релизов нет.
     */
    suspend fun findReleases(malId: Int, titles: List<String>, year: Int?): List<AniLibriaRelease>

    /** Релиз по id со списком серий и HLS-ссылками, запрашивается у AniLibria; `null`, если его нет. Вызывается при запуске серии. */
    suspend fun getRelease(releaseId: Int): AniLibriaRelease?
}

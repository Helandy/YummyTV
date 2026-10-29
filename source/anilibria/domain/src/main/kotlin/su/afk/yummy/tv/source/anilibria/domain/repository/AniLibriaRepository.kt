package su.afk.yummy.tv.source.anilibria.domain.repository

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease

interface AniLibriaRepository {
    /** Релиз по id со списком серий и HLS-ссылками, запрашивается у AniLibria; `null`, если его нет. Вызывается при запуске серии. */
    suspend fun getRelease(releaseId: Int): AniLibriaRelease?
}

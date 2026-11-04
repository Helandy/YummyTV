package su.afk.yummy.tv.source.studiomir.domain.repository

import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreams
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget

interface StudioMirRepository {
    /** Потоки серии; `null`, если тайтла, плеера или серии на сайте нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: StudioMirTarget): StudioMirStreams?
}

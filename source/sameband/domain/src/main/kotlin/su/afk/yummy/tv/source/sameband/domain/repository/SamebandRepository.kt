package su.afk.yummy.tv.source.sameband.domain.repository

import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreams
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget

interface SamebandRepository {
    /** Номера серий из плейлиста плеера тайтла; пусто, если тайтла или плеера на сайте нет. Сбой сети уходит исключением. */
    suspend fun getEpisodes(slug: String): List<Int>

    /** Потоки серии; `null`, если тайтла, плеера или серии на сайте нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: SamebandTarget): SamebandStreams?
}

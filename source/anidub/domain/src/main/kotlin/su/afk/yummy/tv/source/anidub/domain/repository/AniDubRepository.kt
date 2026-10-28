package su.afk.yummy.tv.source.anidub.domain.repository

import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreams
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget

interface AniDubRepository {
    /** Потоки серии; `null`, если тайтла, плеера или серии на сайте нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AniDubTarget): AniDubStreams?
}

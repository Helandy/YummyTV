package su.afk.yummy.tv.source.animeon.domain.repository

import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult

interface AnimeOnRepository {
    /**
     * Потоки серии: [AshdiStreamsResult.NotFound], если серии или файла на сайте нет, [AshdiStreamsResult.RegionBlocked], если
     * плеер закрыт для страны пользователя. Сбой сети уходит исключением.
     */
    suspend fun getStreams(target: AnimeOnTarget): AshdiStreamsResult
}

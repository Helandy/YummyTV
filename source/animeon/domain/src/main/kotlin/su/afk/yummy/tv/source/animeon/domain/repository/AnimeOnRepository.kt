package su.afk.yummy.tv.source.animeon.domain.repository

import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreamsResult
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget

interface AnimeOnRepository {
    /**
     * Потоки серии: [AnimeOnStreamsResult.NotFound], если серии или файла на сайте нет, [AnimeOnStreamsResult.RegionBlocked], если
     * плеер закрыт для страны пользователя. Сбой сети уходит исключением.
     */
    suspend fun getStreams(target: AnimeOnTarget): AnimeOnStreamsResult
}

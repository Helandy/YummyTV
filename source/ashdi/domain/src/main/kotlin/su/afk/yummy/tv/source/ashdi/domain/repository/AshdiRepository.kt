package su.afk.yummy.tv.source.ashdi.domain.repository

import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult

interface AshdiRepository {
    /**
     * Потоки со страницы плеера `https://ashdi.vip/vod/{id}`: [AshdiStreamsResult.NotFound], если страницы нет, адрес чужой или
     * на ней нет файла, [AshdiStreamsResult.RegionBlocked], если страница отдаёт заглушку гео-блока. Сбой сети уходит исключением.
     */
    suspend fun getStreams(pageUrl: String): AshdiStreamsResult
}

package su.afk.yummy.tv.source.ashdi.data.repository

import su.afk.yummy.tv.source.ashdi.data.network.AshdiApi
import su.afk.yummy.tv.source.ashdi.data.utils.ashdiMaster
import su.afk.yummy.tv.source.ashdi.data.utils.ashdiQualities
import su.afk.yummy.tv.source.ashdi.data.utils.ashdiRegionBlocked
import su.afk.yummy.tv.source.ashdi.data.utils.isAshdiVodUrl
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreams
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AshdiRepositoryImpl @Inject constructor(
    private val api: AshdiApi,
) : AshdiRepository {

    /**
     * Страница серии → `file:` в её Playerjs → HLS-мастер. Ссылки CDN не подписаны. Ashdi закрыт для части стран (RU, BY):
     * вместо плеера страница отдаёт заглушку без `file:`, это [AshdiStreamsResult.RegionBlocked], а не «серии нет».
     */
    override suspend fun getStreams(pageUrl: String): AshdiStreamsResult {
        if (!isAshdiVodUrl(pageUrl)) return AshdiStreamsResult.NotFound
        val page = api.getText(pageUrl) ?: return AshdiStreamsResult.NotFound
        val masterUrl = ashdiMaster(page)
            ?: return if (ashdiRegionBlocked(page)) AshdiStreamsResult.RegionBlocked else AshdiStreamsResult.NotFound
        val master = api.getText(masterUrl) ?: return AshdiStreamsResult.NotFound
        return AshdiStreamsResult.Success(AshdiStreams(qualities = ashdiQualities(master), masterUrl = masterUrl))
    }
}

package su.afk.yummy.tv.source.moon.data.repository

import su.afk.yummy.tv.source.moon.data.network.MoonApi
import su.afk.yummy.tv.source.moon.data.utils.moonManifest
import su.afk.yummy.tv.source.moon.data.utils.moonPage
import su.afk.yummy.tv.source.moon.data.utils.moonScript
import su.afk.yummy.tv.source.moon.domain.model.MoonStreams
import su.afk.yummy.tv.source.moon.domain.model.MoonStreamsResult
import su.afk.yummy.tv.source.moon.domain.repository.MoonRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MoonRepositoryImpl @Inject constructor(
    private val api: MoonApi,
) : MoonRepository {

    /** Страница → скрипт плеера из `atob` → манифест. Потоку нужны Referer страницы плеера и Origin Moon. */
    override suspend fun getStreams(pageUrl: String): MoonStreamsResult {
        val moonPage = moonPage(pageUrl) ?: return MoonStreamsResult.Failed("moon: bad url")
        val cleanUrl = moonPage.url
        val page = api.getPage(cleanUrl) ?: return MoonStreamsResult.NotFound
        val script = moonScript(page) ?: return MoonStreamsResult.Failed("moon: payload")
        val manifest = moonManifest(script) ?: return MoonStreamsResult.Failed("moon: manifest")
        return MoonStreamsResult.Success(MoonStreams(manifest.url, MoonApi.streamHeaders(cleanUrl, moonPage.origin), manifest.qualities))
    }
}

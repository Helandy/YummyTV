package su.afk.yummy.tv.data.details.network

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.data.details.dto.EpisodeFlagsBackupDto

/** Резерв филлеров и рекапов: копия Tenrai на YummyTV API. Нужен, только если сам Tenrai не ответил. */
class YummyEpisodeFlagsApi(
    private val clientProvider: YaniHttpClientProvider,
    private val siteApiConfig: SiteApiConfig,
) {
    /** Пометки тайтла; 404 (копии ещё нет) — это отсутствие данных, а не пустой список, поэтому бросается как ошибка. */
    suspend fun getFlags(malId: Int): EpisodeFlagsBackupDto {
        val url = "${siteApiConfig.baseUrl}/episode-flags/$malId"
        val response = clientProvider.get().get(url) {
            expectSuccess = false
        }
        return when {
            response.status.isSuccess() -> response.body()
            response.status == HttpStatusCode.NotFound -> error("YummyTV API: для MAL $malId резерва флагов нет")
            else -> error("YummyTV API ${response.status}: ${response.bodyAsText()}")
        }
    }
}

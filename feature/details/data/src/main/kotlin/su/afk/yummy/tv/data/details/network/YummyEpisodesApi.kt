package su.afk.yummy.tv.data.details.network

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.network.yummy.signSiteRequest
import su.afk.yummy.tv.data.details.dto.YummyEpisodesDto

/**
 * Названия и описания серий по MyAnimeList id.
 * Клиент общий с yani: заголовки yani подставляются только для api.yani.tv.
 */
class YummyEpisodesApi(
    private val clientProvider: YaniHttpClientProvider,
    private val siteApiConfig: SiteApiConfig,
    private val signers: Set<SiteRequestSigner> = emptySet(),
) {
    /**
     * Серии записи. Смещение серий франшизы, которую TMDB хранит сквозной нумерацией, сервер считает сам.
     * 404 (нет маппинга MAL→TMDB либо смещение посчитать не удалось) — валидный ответ «серий нет», а не ошибка.
     */
    suspend fun getEpisodes(malId: Int): YummyEpisodesDto {
        val url = "${siteApiConfig.baseUrl}/anime/mal/$malId"
        val response = clientProvider.get().get(url) {
            signSiteRequest(signers, "GET", url)
            // Статус разбирается ниже: 404 — «серий нет», а не ошибка.
            expectSuccess = false
        }
        return when {
            response.status.isSuccess() -> response.body()
            response.status == HttpStatusCode.NotFound -> YummyEpisodesDto(malId = malId)
            else -> error("YummyTV API ${response.status}: ${response.bodyAsText()}")
        }
    }
}

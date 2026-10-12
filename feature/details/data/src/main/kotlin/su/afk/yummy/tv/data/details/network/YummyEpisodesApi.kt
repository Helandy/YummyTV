package su.afk.yummy.tv.data.details.network

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.network.yummy.YUMMY_TV_API_BASE_URL
import su.afk.yummy.tv.data.details.dto.YummyEpisodesDto

/**
 * Названия и описания серий по MyAnimeList id.
 * Клиент общий с yani: заголовки yani подставляются только для api.yani.tv.
 */
class YummyEpisodesApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    /** 404 (нет маппинга MAL→TMDB) — валидный ответ «серий нет», а не ошибка. */
    suspend fun getEpisodes(malId: Int): YummyEpisodesDto {
        val response = clientProvider.get().get("$YUMMY_TV_API_BASE_URL/anime/mal/$malId") {
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

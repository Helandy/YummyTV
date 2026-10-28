package su.afk.yummy.tv.data.details.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.data.details.dto.MalEpisodeDto
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto
import java.io.IOException
import javax.inject.Inject

/**
 * Любой API со схемой Jikan v4. Публичный Jikan закрыт 1 октября 2026, поэтому используем Tenrai: те же ответы,
 * без ключа до 4 запросов в секунду и 120 в минуту.
 */
private const val BASE_URL = "https://api.tenrai.org/v1"
private const val PAGE_DELAY_MS = 300L
private const val MAX_PAGES = 30

/** Пометки серий (филлер/рекап) из MyAnimeList. */
class MalEpisodeFlagsApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /**
     * Все серии тайтла, склеенные из страниц. 404 — «серий нет». Ошибка любой страницы (в том числе 429) пробрасывается:
     * неполный список в кэш попасть не должен.
     */
    suspend fun getEpisodes(malId: Int): MalEpisodesDto {
        val episodes = mutableListOf<MalEpisodeDto>()
        for (page in 1..MAX_PAGES) {
            if (page > 1) delay(PAGE_DELAY_MS)
            val response = client.get("$BASE_URL/anime/$malId/episodes") {
                parameter("page", page)
                expectSuccess = false
            }
            if (response.status == HttpStatusCode.NotFound) break
            if (!response.status.isSuccess()) throw IOException("Episodes API: HTTP ${response.status.value}")
            val body = response.body<MalEpisodesDto>()
            episodes += body.data
            if (!body.pagination.hasNextPage) break
        }
        return MalEpisodesDto(data = episodes)
    }
}

package su.afk.yummy.tv.source.moon.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Страницы плеера Moon (moonanime.art и зеркала). Страница отдаётся с Referer сайта Mikai, на котором плеер встроен. */
internal class MoonApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст страницы; `null` при 404. */
    suspend fun getPage(url: String): String? {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Referrer, "https://mikai.me/")
            CLIENT_HEADERS.forEach { (name, value) -> header(name, value) }
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Moon: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    companion object {
        /** Без этих заголовков moonanime.art отвечает 400, а CDN потока — 403: набор повторяет запросы плеера на странице Mikai. */
        val CLIENT_HEADERS: Map<String, String> = mapOf(
            "Accept" to "*/*",
            "Accept-Language" to "uk-UA,uk;q=0.9,en;q=0.8",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-site",
            "sec-ch-ua" to "\"Chromium\";v=\"124\", \"Not:A-Brand\";v=\"8\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Linux\"",
        )

        const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        /** Заголовки потока: CDN принимает только запрос «со страницы плеера» ([origin] — хост этой страницы). */
        fun streamHeaders(pageUrl: String, origin: String): Map<String, String> =
            CLIENT_HEADERS + mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to pageUrl,
                "Origin" to origin,
            )
    }
}

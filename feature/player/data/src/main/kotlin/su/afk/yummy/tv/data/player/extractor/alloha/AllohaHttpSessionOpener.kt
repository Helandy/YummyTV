package su.afk.yummy.tv.data.player.extractor.alloha

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import java.io.IOException
import java.util.Locale

/** What one HTTP round trip to the player produced: parsed sources plus the headers the CDN expects. */
internal data class AllohaOpenedSession(
    val sources: AllohaParsedSources,
    val headers: Map<String, String>,
)

/**
 * Opens an Alloha session with plain HTTP, no browser: fetch the player page, read its per-load
 * seed, sign the `bnsi` request with the `Borth` header and read the dubbing/quality ladder from
 * the answer. The URLs in that answer play as they are, so there is no page to keep alive and
 * nothing to rotate: a session that stopped working is replaced by opening a new one.
 *
 * Blocking: call from an IO dispatcher.
 */
internal class AllohaHttpSessionOpener(
    private val client: OkHttpClient,
    private val userAgent: String,
    private val analytics: AnalyticsTracker,
) {
    @Throws(IOException::class)
    fun open(iframeUrl: String): AllohaOpenedSession {
        val page = fetchPage(iframeUrl)
        val viewporti = VIEWPORTI.find(page)?.groupValues?.get(1)
            ?: throw IOException("Alloha: viewporti seed is missing")
        val token = iframeUrl.toHttpUrl().queryParameter("token")
            ?: throw IOException("Alloha: token is missing in iframe url")
        val activeId = parseActiveId(page)
            ?: throw IOException("Alloha: active media id is missing")

        val origin = originOf(iframeUrl)
        val bnsi = fetchBnsi(origin, iframeUrl, activeId, token, allohaBorthHeader(viewporti))
        val sources = try {
            parseSources(bnsi, analytics)
        } catch (error: JSONException) {
            throw IOException("Alloha: bnsi is not valid json", error)
        }
        return AllohaOpenedSession(sources = sources, headers = allohaCdnHeaders(origin, userAgent))
    }

    private fun fetchPage(iframeUrl: String): String {
        val request = Request.Builder().url(iframeUrl)
            .header("Referer", PAGE_REFERER)
            .header("User-Agent", userAgent)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Alloha: player page HTTP ${response.code}")
            response.body.string()
        }
    }

    private fun fetchBnsi(
        origin: String,
        iframeUrl: String,
        activeId: Long,
        token: String,
        borth: String,
    ): String {
        val body = FormBody.Builder()
            .add("token", token)
            .add("av1", "false")
            .add("autoplay", "0")
            .add("audio", "")
            .add("subtitle", "")
            .build()
        val request = Request.Builder().url("$origin/bnsi/movies/$activeId")
            .post(body)
            .header("User-Agent", userAgent)
            .header("Borth", borth)
            .header("Referer", iframeUrl)
            .header("Origin", origin)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Alloha: bnsi HTTP ${response.code}")
            response.body.string().also {
                if (it.isBlank()) throw IOException("Alloha: bnsi answered with an empty body")
            }
        }
    }

    private fun originOf(iframeUrl: String): String {
        val url = iframeUrl.toHttpUrl()
        return "${url.scheme}://${url.host.lowercase(Locale.ROOT)}"
    }

    private fun parseActiveId(page: String): Long? {
        val literal = FILE_LIST.find(page)?.groupValues?.get(2) ?: return null
        val json = literal.replace("\\\"", "\"").replace("\\\\", "\\")
        return runCatching { JSONObject(json).getJSONObject("active").getLong("id") }.getOrNull()
    }

    private companion object {
        // The page is served only to a visitor that arrives from the site that embeds it.
        const val PAGE_REFERER = "https://ru.yummyani.me/"
        const val ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"
        val VIEWPORTI = Regex("""<meta\s+name="viewporti"\s+content="([^"]+)"""")
        val FILE_LIST = Regex(
            """const\s+fileList\s*=\s*JSON\.parse\(\s*(["'`])(.*?)\1\s*\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}

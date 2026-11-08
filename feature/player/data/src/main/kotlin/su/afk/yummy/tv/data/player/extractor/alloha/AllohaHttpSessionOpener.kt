// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.data.player.extractor.alloha

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import su.afk.yummy.tv.core.utils.network.HttpHeaderNames

/** What one HTTP round trip to the player produced: parsed sources plus the headers the CDN expects. */
internal data class AllohaOpenedSession(
    val sources: AllohaParsedSources,
    val headers: Map<String, String>,
    /** Where to open the player's WebSocket; null when `bnsi` did not offer one. */
    val socketTicket: AllohaSocketTicket? = null,
    /** What the page's HTTP view reports need; null when the page did not carry it. */
    val viewTicket: AllohaViewTicket? = null,
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
    private val fingerprint: String,
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
                val headers = allohaCdnHeaders(origin, userAgent) + authorizationHeader(origin, page, iframeUrl)
        val bnsi = fetchBnsi(origin, iframeUrl, activeId, token, allohaBorthHeader(fingerprint, viewporti))
        val picker = AllohaMirrorPicker { url -> isReachable(url, headers) }
        val sources = try {
            parseSources(bnsi, analytics) { mirrors ->
                picker.pick(mirrors).also { picked ->
                    analytics.log(LOG_TAG) { "mirrors=${mirrors.map { it.hostOf() }} picked=${picked.hostOf()}" }
                }
            }
        } catch (error: JSONException) {
            throw IOException("Alloha: bnsi is not valid json", error)
        }
        return AllohaOpenedSession(sources = sources, headers = headers, socketTicket = parseSocketTicket(bnsi), viewTicket = parseViewTicket(page, activeId))
    }

    /**
     * Whether the CDN serves [url] to a request that looks like the player's. The playlist is a few
     * hundred bytes and only the status is read, so a mirror that answers 403 or does not answer
     * within [MIRROR_PROBE_TIMEOUT_MS] is skipped.
     */
    private fun isReachable(url: String, headers: Map<String, String>): Boolean = try {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        client.newBuilder().callTimeout(MIRROR_PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS).build()
            .newCall(request).execute().use { it.isSuccessful }
    } catch (error: IOException) {
        false
    }.also { reachable ->
        if (!reachable) analytics.log(LOG_TAG) { "mirror is not reachable, using the next one host=${url.hostOf()}" }
    }

    /**
     * The player page's own script carries a long constant that the site sends to the CDN as
     * `Authorizations: Bearer <key>` on every media request, next to `Accepts-Controls`. The script
     * name carries a content hash, so the key is read once per script.
     */
    private fun authorizationHeader(origin: String, page: String, iframeUrl: String): Map<String, String> {
        val path = APP_SCRIPT.find(page)?.groupValues?.get(1) ?: return emptyMap()
        val scriptUrl = if (path.startsWith("http")) path else origin + "/" + path.trimStart('.', '/')
        val key = apiKeyCache[scriptUrl] ?: run {
            val request = Request.Builder().url(scriptUrl)
                .header(HttpHeaderNames.USER_AGENT, userAgent)
                .header("Referer", iframeUrl)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .build()
            val body = try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body.string() else null
                }
            } catch (error: IOException) {
                null
            }
            body?.let { API_KEY.find(it)?.groupValues?.get(1) }?.also { apiKeyCache[scriptUrl] = it }
        }
        analytics.log(LOG_TAG) { "authorization key present=${key != null}" }
        return key?.let { mapOf("authorizations" to "Bearer $it") }.orEmpty()
    }

    private fun fetchPage(iframeUrl: String): String {
        val request = Request.Builder().url(iframeUrl)
            .header("Referer", PAGE_REFERER)
            .header(HttpHeaderNames.USER_AGENT, userAgent)
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
            .header(HttpHeaderNames.USER_AGENT, userAgent)
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

    private fun parseViewTicket(page: String, activeId: Long): AllohaViewTicket? {
        val block = USER_PARAM.find(page)?.value ?: return null
        val token = PARAM_TOKEN.find(block)?.groupValues?.get(1) ?: return null
        val domain = PARAM_DOMAIN.find(block)?.groupValues?.get(1) ?: return null
        return AllohaViewTicket(token = token, domain = domain, fileId = activeId.toString())
    }

    private fun parseSocketTicket(bnsi: String): AllohaSocketTicket? {
        val root = runCatching { JSONObject(bnsi) }.getOrNull() ?: return null
        fun field(name: String) = if (root.isNull(name)) null else root.optString(name).takeIf(String::isNotBlank)
        val url = field("pnr") ?: return null
        val key = field("pnk") ?: return null
        return AllohaSocketTicket(url = url, key = key)
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

    private fun String.hostOf(): String = runCatching { toHttpUrl().host }.getOrDefault("unknown")

    private companion object {
        val apiKeyCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        val APP_SCRIPT = Regex("""<script[^>]+src=["']([^"']*/app\.[^"']*\.js[^"']*)["']""")
        val API_KEY = Regex("""['"]([a-zA-Z0-9]{340,420})['"]""")
        const val LOG_TAG = PersistedLogTags.ALLOHA_EXTRACTOR
        const val MIRROR_PROBE_TIMEOUT_MS = 4_000L

        // The page is served only to a visitor that arrives from the site that embeds it.
        const val PAGE_REFERER = "https://ru.yummyani.me/"
        const val ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"
        val USER_PARAM = Regex("userParam\\s*=\\s*\\{[^}]*\\}")
        val PARAM_TOKEN = Regex("token:\\s*'([^']+)'")
        val PARAM_DOMAIN = Regex("domain:\\s*'([^']*)'")
        val VIEWPORTI = Regex("""<meta\s+name="viewporti"\s+content="([^"]+)"""")
        val FILE_LIST = Regex(
            """const\s+fileList\s*=\s*JSON\.parse\(\s*(["'`])(.*?)\1\s*\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}

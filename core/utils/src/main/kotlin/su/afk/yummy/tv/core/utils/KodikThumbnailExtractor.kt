package su.afk.yummy.tv.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object KodikThumbnailExtractor {

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun extract(iframeUrl: String): String? {
        cache[iframeUrl]?.let { return it.ifEmpty { null } }
        return withContext(Dispatchers.IO) {
            val result = try {
                val html = fetchHtml(normalizeUrl(iframeUrl))
                parsePosterUrl(html)?.let { toHttpsUrl(it) }
            } catch (_: Exception) {
                null
            }
            cache[iframeUrl] = result ?: ""
            result
        }
    }

    private fun parsePosterUrl(html: String): String? =
        Regex("""['"]((https?:)?//[^'" ]*thumb001\.[a-z]+)['"]""")
            .find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http") -> url
        else -> "https://$url"
    }

    private fun toHttpsUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> url.replaceFirst("http://", "https://")
        else -> url
    }

    private fun fetchHtml(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 10_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Referer", "https://yani.tv/")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
        return conn.inputStream.bufferedReader().readText()
    }
}

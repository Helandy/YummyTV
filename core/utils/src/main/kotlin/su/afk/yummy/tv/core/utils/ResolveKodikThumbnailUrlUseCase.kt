package su.afk.yummy.tv.core.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/** Резолвит прямой URL превью серии из Kodik iframe. */
class ResolveKodikThumbnailUrlUseCase @Inject constructor() {

    suspend operator fun invoke(iframeUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val html = fetchHtml(normalizeIframeUrl(iframeUrl))
            parsePosterUrl(html)?.let(::toHttpsUrl)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchHtml(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Referer", "https://yani.tv/")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 10_000
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"
    }
}

fun normalizeIframeUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http") -> url
    else -> "https://$url"
}

private fun toHttpsUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> url.replaceFirst("http://", "https://")
    else -> url
}

private fun parsePosterUrl(html: String): String? =
    Regex("""['"]((https?:)?//[^'" ]*thumb001\.[a-z]+)['"]""")
        .find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

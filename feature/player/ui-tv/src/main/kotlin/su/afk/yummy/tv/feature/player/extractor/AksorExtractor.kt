package su.afk.yummy.tv.feature.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.feature.player.view.CHROME_UA
import java.net.HttpURLConnection
import java.net.URL

internal object AksorExtractor {

    private val QUALITY_PRIORITY = listOf("q2k", "q4k", "q1080", "q720", "q480", "q360")

    suspend fun extract(iframeUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val hash = iframeUrl.trimEnd('/').substringAfterLast('/')
            if (hash.isBlank()) {
                return@withContext null
            }

            val apiUrl = "https://player.aksor.tv/api/video/$hash"

            val json = fetchJson(apiUrl, referer = "https://yani.tv/")
            val qualities = json.optJSONObject("qualities") ?: run {
                return@withContext null
            }

            val streamUrl = QUALITY_PRIORITY.firstNotNullOfOrNull { key ->
                qualities.optString(key).takeIf { it.isNotEmpty() && it != "null" }
            }

            streamUrl
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchJson(url: String, referer: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Referer", referer)
        conn.setRequestProperty("User-Agent", CHROME_UA)
        conn.setRequestProperty("Accept", "application/json")
        val body = conn.inputStream.bufferedReader().readText()
        return JSONObject(body)
    }
}

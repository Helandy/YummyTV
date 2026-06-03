package su.afk.yummy.tv.feature.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.feature.player.view.CHROME_UA
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

// CVH (CdnVideoHub) iframe URL format:
//   //ru.yummyani.me/iframeCVH.html?dubbing_code=AnilibriaTV&anime_id=31240&episode=1
//
// Flow: playlist API → find vkId by episode+voice → video API → hlsUrl
internal object CvhExtractor {

    private const val PLAYLIST_URL = "https://plapi.cdnvideohub.com/api/v1/player/sv/playlist"
    private const val VIDEO_URL = "https://plapi.cdnvideohub.com/api/v1/player/sv/video"
    private const val PUBLISHER_ID = "745"
    private const val AGGREGATOR = "mali"
    private const val REFERER = "https://ru.yummyani.me/"

    suspend fun extract(iframeUrl: String, autoQualityLabel: String): LinkedHashMap<String, String>? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl
            val query = fullUrl.substringAfter("?", "")
            val params = parseQuery(query)

            val animeId = params["anime_id"] ?: run {
                return@withContext null
            }
            val episodeStr = params["episode"] ?: "1"
            val episodeNum = episodeStr.toIntOrNull() ?: 1
            val dubbingCode = params["dubbing_code"] ?: ""

            val playlistJson = fetchJson(
                "$PLAYLIST_URL?pub=$PUBLISHER_ID&id=$animeId&aggr=$AGGREGATOR",
                referer = REFERER,
            )
            val items = playlistJson.optJSONArray("items") ?: run {
                return@withContext null
            }

            // Collect items matching the episode number
            val candidates = mutableListOf<JSONObject>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                if (item.optInt("episode") == episodeNum) candidates += item
            }

            if (candidates.isEmpty()) {
                return@withContext null
            }

            // Prefer item whose voiceStudio matches dubbing code
            val item = candidates.firstOrNull { item ->
                item.optString("voiceStudio").equals(dubbingCode, ignoreCase = true)
            } ?: candidates.first()

            val vkId = item.optString("vkId").takeIf { it.isNotEmpty() } ?: run {
                return@withContext null
            }

            val videoJson = fetchJson("$VIDEO_URL/$vkId", referer = REFERER)
            val sources = videoJson.optJSONObject("sources") ?: run {
                return@withContext null
            }

            // Auto HLS first so keys.last() = best available MP4 (used as default quality)
            val qualities = LinkedHashMap<String, String>()
            sources.optString("hlsUrl").takeIf { it.isNotEmpty() }?.let { qualities[autoQualityLabel] = it }
            sources.optString("mpegLowestUrl").takeIf { it.isNotEmpty() }?.let { qualities["240p"] = it }
            sources.optString("mpegLowUrl").takeIf { it.isNotEmpty() }?.let { qualities["360p"] = it }
            sources.optString("mpegMediumUrl").takeIf { it.isNotEmpty() }?.let { qualities["480p"] = it }
            sources.optString("mpegHighUrl").takeIf { it.isNotEmpty() }?.let { qualities["720p"] = it }
            sources.optString("mpegFullHdUrl").takeIf { it.isNotEmpty() }?.let { qualities["1080p"] = it }

            if (qualities.isEmpty()) {
                return@withContext null
            }
            qualities
        } catch (e: Exception) {
            logExtractorFailure("CVH", iframeUrl, "unexpected extractor error", e)
            null
        }
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&").mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq < 0) null
            else pair.substring(0, eq) to URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
        }.toMap()

    private fun fetchJson(url: String, referer: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Referer", referer)
            conn.setRequestProperty("User-Agent", CHROME_UA)
            conn.setRequestProperty("Accept", "application/json")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }
}

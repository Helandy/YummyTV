package su.afk.yummy.tv.data.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.data.player.utils.BROWSER_STREAM_HEADERS
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import su.afk.yummy.tv.domain.player.isCvhPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import java.net.URLDecoder
import javax.inject.Inject

// CVH (CdnVideoHub) iframe URL format:
//   //ru.yummyani.me/iframeCVH.html?dubbing_code=AnilibriaTV&anime_id=31240&episode=1
//
// Flow: playlist API → find vkId by episode+voice → video API → hlsUrl
internal class CvhExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
) : PlayerStreamExtractor {

    private val PLAYLIST_URL = "https://plapi.cdnvideohub.com/api/v1/player/sv/playlist"
    private val VIDEO_URL = "https://plapi.cdnvideohub.com/api/v1/player/sv/video"
    private val PUBLISHER_ID = "745"
    private val AGGREGATOR = "mali"
    private val REFERER = "https://ru.yummyani.me/"

    override fun supports(url: String): Boolean = url.isCvhPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: android.content.Context,
    ): PlayerStreamResolveResult =
        extractQualities(
            iframeUrl = request.iframeUrl,
        )?.let { qualities ->
            PlayerStreamResolveResult.Stream(
                url = qualities.values.last(),
                headers = BROWSER_STREAM_HEADERS,
                qualities = qualities,
            )
        } ?: PlayerStreamResolveResult.Failed

    private suspend fun extractQualities(
        iframeUrl: String,
    ): LinkedHashMap<String, String>? = withContext(Dispatchers.IO) {
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
            val failoverHost = videoJson.optString("failoverHost").takeIf { it.isNotBlank() }
            val sources = videoJson.optJSONObject("sources") ?: run {
                return@withContext null
            }

            // No Auto/HLS entry: CdnVideoHub's HLS manifests embed raw CDN-IP segment
            // URLs that aren't rewritten to failoverHost, which breaks HLS downloads.
            // MP4 qualities only; keys.last() = best available (used as default quality).
            val qualities = LinkedHashMap<String, String>()
            qualities.putCvhQuality("240p", sources.optString("mpegLowestUrl"), failoverHost)
            qualities.putCvhQuality("360p", sources.optString("mpegLowUrl"), failoverHost)
            qualities.putCvhQuality("480p", sources.optString("mpegMediumUrl"), failoverHost)
            qualities.putCvhQuality("720p", sources.optString("mpegHighUrl"), failoverHost)
            qualities.putCvhQuality("1080p", sources.optString("mpegFullHdUrl"), failoverHost)

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

    private suspend fun fetchJson(url: String, referer: String): JSONObject =
        JSONObject(
            httpClient.getText(
                url = url,
                headers = mapOf(
                    "Referer" to referer,
                    "User-Agent" to CHROME_UA,
                    "Accept" to "application/json",
                ),
            ).body,
        )

    private fun LinkedHashMap<String, String>.putCvhQuality(
        label: String,
        url: String,
        failoverHost: String?,
    ) {
        val sourceUrl = url.takeIf { it.isNotBlank() } ?: return
        this[label] = normalizeOkCdnIpUrl(sourceUrl, failoverHost)
    }

    private fun normalizeOkCdnIpUrl(url: String, failoverHost: String?): String {
        val host = failoverHost?.takeIf { it.isNotBlank() } ?: return url
        val parsed = runCatching { URL(url) }.getOrNull() ?: return url
        if (!parsed.protocol.equals("https", ignoreCase = true)) return url
        if (!IPV4_HOST_REGEX.matches(parsed.host)) return url

        return runCatching {
            URL(parsed.protocol, host, parsed.port, parsed.file).toString()
        }.getOrDefault(url)
    }

    private val IPV4_HOST_REGEX = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
}

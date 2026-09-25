package su.afk.yummy.tv.data.player.extractor.cvh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.fetchJson
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.data.player.network.BROWSER_STREAM_HEADERS
import su.afk.yummy.tv.data.player.network.CHROME_UA
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
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
    private val analyticsTracker: AnalyticsTracker,
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
                logFailure(iframeUrl, "missing anime_id")
                return@withContext null
            }
            val episodeStr = params["episode"] ?: "1"
            val episodeNum = episodeStr.toIntOrNull() ?: 1
            val dubbingCode = params["dubbing_code"] ?: ""
            val dubbingLabel = params["dubbing"] ?: ""

            val playlistJson = httpClient.fetchJson(
                url = "$PLAYLIST_URL?pub=$PUBLISHER_ID&id=$animeId&aggr=$AGGREGATOR",
                headers = jsonHeaders(REFERER),
            )
            val items = playlistJson.optJSONArray("items") ?: run {
                logFailure(iframeUrl, "playlist has no items")
                return@withContext null
            }
            // Default true keeps the pre-existing episode filtering if the field ever disappears.
            val isSerial = playlistJson.optBoolean("isSerial", true)

            val playlistItems = (0 until items.length()).mapNotNull { index ->
                val item = items.optJSONObject(index) ?: return@mapNotNull null
                CvhPlaylistItem(
                    vkId = item.optStringOrEmpty("vkId"),
                    voiceStudio = item.optStringOrEmpty("voiceStudio"),
                    voiceType = item.optStringOrEmpty("voiceType"),
                    episode = if (item.isNull("episode")) null else item.optInt("episode"),
                )
            }

            val item = selectCvhItem(
                items = playlistItems,
                isSerial = isSerial,
                episodeNum = episodeNum,
                dubbingCode = dubbingCode,
                dubbingLabel = dubbingLabel,
            ) ?: run {
                logFailure(
                    iframeUrl,
                    "no playlist item for episode $episodeNum " +
                        "(isSerial=$isSerial, items=${playlistItems.size})",
                )
                return@withContext null
            }

            val vkId = item.vkId.takeIf { it.isNotEmpty() } ?: run {
                logFailure(iframeUrl, "playlist item has no vkId")
                return@withContext null
            }

            val videoJson =
                httpClient.fetchJson(url = "$VIDEO_URL/$vkId", headers = jsonHeaders(REFERER))
            val failoverHost = videoJson.optString("failoverHost").takeIf { it.isNotBlank() }
            val sources = videoJson.optJSONObject("sources") ?: run {
                logFailure(iframeUrl, "video response has no sources")
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
                logFailure(iframeUrl, "no mp4 qualities in sources")
                return@withContext null
            }
            qualities
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            analyticsTracker.logExtractorFailure("CVH", iframeUrl, "unexpected extractor error", e)
            null
        }
    }

    // optString maps an explicit JSON null to the literal "null" - the subtitles entry ships
    // "voiceStudio": null, and that string would otherwise leak into the voice matching.
    private fun JSONObject.optStringOrEmpty(key: String): String =
        if (isNull(key)) "" else optString(key)

    private fun logFailure(iframeUrl: String, reason: String) {
        analyticsTracker.logExtractorFailure("CVH", iframeUrl, reason)
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&").mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq < 0) {
                null
            } else {
                pair.substring(0, eq) to URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
            }
        }.toMap()

    private fun jsonHeaders(referer: String): Map<String, String> = mapOf(
        "Referer" to referer,
        "User-Agent" to CHROME_UA,
        "Accept" to "application/json",
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

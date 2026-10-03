package su.afk.yummy.tv.data.player.extractor.cvh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.player.isCvhPlayerUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.fetchJson
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.data.player.network.BROWSER_STREAM_HEADERS
import su.afk.yummy.tv.data.player.network.CHROME_UA
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
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
            useFailoverHost = request.forceRefresh,
        )

    /**
     * @param useFailoverHost перерезолв после сбоя воспроизведения: основной узел okcdn мог
     *   обрывать ответы, поэтому все качества переводятся на failoverHost из ответа API — он
     *   отдаёт тот же файл по той же подписанной ссылке.
     */
    private suspend fun extractQualities(
        iframeUrl: String,
        useFailoverHost: Boolean,
    ): PlayerStreamResolveResult = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl
            val query = fullUrl.substringAfter("?", "")
            val params = parseQuery(query)

            val animeId = params["anime_id"] ?: run {
                logFailure(iframeUrl, "missing anime_id")
                return@withContext PlayerStreamResolveResult.Failed
            }
            val episodeStr = params["episode"] ?: "1"
            val episodeNum = episodeStr.toIntOrNull() ?: 1
            val dubbingCode = params["dubbing_code"] ?: ""
            val dubbingLabel = params["dubbing"] ?: ""

            val playlistJson = fetchJsonWithRetry(
                url = "$PLAYLIST_URL?pub=$PUBLISHER_ID&id=$animeId&aggr=$AGGREGATOR",
            )
            val items = playlistJson.optJSONArray("items") ?: run {
                logFailure(iframeUrl, "playlist has no items")
                return@withContext PlayerStreamResolveResult.Unavailable()
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
                return@withContext PlayerStreamResolveResult.Unavailable()
            }

            val vkId = item.vkId.takeIf { it.isNotEmpty() } ?: run {
                logFailure(iframeUrl, "playlist item has no vkId")
                return@withContext PlayerStreamResolveResult.Failed
            }

            val videoJson =
                fetchJsonWithRetry(url = "$VIDEO_URL/$vkId")
            val failoverHost = videoJson.optString("failoverHost").takeIf { it.isNotBlank() }
            val sources = videoJson.optJSONObject("sources") ?: run {
                logFailure(iframeUrl, "video response has no sources")
                return@withContext PlayerStreamResolveResult.Failed
            }

            // No Auto/HLS entry: CdnVideoHub's HLS manifests embed raw CDN-IP segment
            // URLs that aren't rewritten to failoverHost, which breaks HLS downloads.
            // MP4 qualities only; keys.last() = best available (used as default quality).
            val qualities = LinkedHashMap<String, String>()
            qualities.putCvhQuality("240p", sources.optString("mpegLowestUrl"), failoverHost, useFailoverHost)
            qualities.putCvhQuality("360p", sources.optString("mpegLowUrl"), failoverHost, useFailoverHost)
            qualities.putCvhQuality("480p", sources.optString("mpegMediumUrl"), failoverHost, useFailoverHost)
            qualities.putCvhQuality("720p", sources.optString("mpegHighUrl"), failoverHost, useFailoverHost)
            qualities.putCvhQuality("1080p", sources.optString("mpegFullHdUrl"), failoverHost, useFailoverHost)

            if (qualities.isEmpty()) {
                logFailure(iframeUrl, "no mp4 qualities in sources")
                return@withContext PlayerStreamResolveResult.Failed
            }
            analyticsTracker.log("CvhExtractor") {
                "Resolved vkId=$vkId episode=$episodeNum dubbing=$dubbingCode " +
                    "qualities=${qualities.keys} failoverHost=$failoverHost " +
                    "useFailoverHost=$useFailoverHost hosts=${qualities.values.map(::hostOf).distinct()}"
            }
            PlayerStreamResolveResult.Stream(
                url = qualities.values.last(),
                headers = BROWSER_STREAM_HEADERS,
                qualities = qualities,
            )
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            analyticsTracker.logExtractorFailure("CVH", iframeUrl, "unexpected extractor error", e)
            PlayerStreamResolveResult.Failed
        }
    }

    /**
     * HTTP-ошибка бросает исключение (а не маскируется под JSONException на HTML-теле), один
     * повтор после короткой паузы закрывает кратковременные сбои сети на слабых приставках.
     */
    private suspend fun fetchJsonWithRetry(url: String): JSONObject {
        val headers = jsonHeaders(REFERER)
        return try {
            httpClient.fetchJson(url = url, headers = headers, throwOnFailure = true)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            delay(RETRY_DELAY_MS)
            httpClient.fetchJson(url = url, headers = headers, throwOnFailure = true)
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
        useFailoverHost: Boolean,
    ) {
        val sourceUrl = url.takeIf { it.isNotBlank() } ?: return
        this[label] = normalizeOkCdnHost(sourceUrl, failoverHost, useFailoverHost)
    }

    /**
     * Сырые IP-адреса CDN всегда меняются на failoverHost; именованный узел — только когда
     * [forceFailover] (перерезолв после сбоя).
     */
    private fun normalizeOkCdnHost(
        url: String,
        failoverHost: String?,
        forceFailover: Boolean,
    ): String {
        val host = failoverHost?.takeIf { it.isNotBlank() } ?: return url
        val parsed = runCatching { URL(url) }.getOrNull() ?: return url
        if (!parsed.protocol.equals("https", ignoreCase = true)) return url
        if (!forceFailover && !IPV4_HOST_REGEX.matches(parsed.host)) return url

        return runCatching {
            URL(parsed.protocol, host, parsed.port, parsed.file).toString()
        }.getOrDefault(url)
    }

    private fun hostOf(url: String): String? = runCatching { URL(url).host }.getOrNull()

    private val IPV4_HOST_REGEX = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
    private val RETRY_DELAY_MS = 700L
}

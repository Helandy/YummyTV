package su.afk.yummy.tv.data.player.extractor.rutube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.ExtractedStream
import su.afk.yummy.tv.data.player.extractor.common.fetchText
import su.afk.yummy.tv.data.player.extractor.common.hasKnownUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.data.player.extractor.common.normalizeUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.orderQualityMap
import su.afk.yummy.tv.data.player.extractor.common.resolveRelativeUrl
import su.afk.yummy.tv.data.player.extractor.common.withAutoQualityLabel
import su.afk.yummy.tv.data.player.network.CHROME_UA
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.domain.player.isRutubePlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import javax.inject.Inject

internal class RutubeExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
    private val analyticsTracker: AnalyticsTracker,
) : PlayerStreamExtractor {

    private val RUTUBE_ORIGIN = "https://rutube.ru"
    private val OPTIONS_URL = "$RUTUBE_ORIGIN/api/play/options/%s/?no_404=true"
    private val VIDEO_ID_PATTERN = Regex("(?i)([a-f0-9]{32})")
    private val RESOLUTION_PATTERN = Regex("(?i)RESOLUTION=(\\d+)x(\\d+)")

    override fun supports(url: String): Boolean = url.isRutubePlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: android.content.Context,
    ): PlayerStreamResolveResult =
        extractStream(
            iframeUrl = request.iframeUrl,
            autoQualityLabel = request.autoQualityLabel,
        )?.toResolveResult() ?: PlayerStreamResolveResult.Failed

    private suspend fun extractStream(
        iframeUrl: String,
        autoQualityLabel: String = "auto"
    ): ExtractedStream? = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(iframeUrl)

        try {
            val videoId = extractVideoId(normalizedUrl) ?: run {
                analyticsTracker.logExtractorFailure("Rutube", normalizedUrl, "video id not found")
                return@withContext null
            }
            val headers = streamHeaders(normalizedUrl)
            val options = JSONObject(
                httpClient.fetchText(
                    url = OPTIONS_URL.format(videoId),
                    headers = fetchHeaders(normalizedUrl),
                    throwOnFailure = true,
                ),
            )
            val streamUrl = options.optJSONObject("video_balancer")
                ?.let { balancer ->
                    balancer.optString("m3u8")
                        .ifBlank { balancer.optString("default") }
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                }
                ?: run {
                    analyticsTracker.logExtractorFailure(
                        "Rutube",
                        normalizedUrl,
                        "video_balancer stream not found"
                    )
                    return@withContext null
                }

            val qualities = buildQualityMap(
                streamUrl = streamUrl,
                referer = normalizedUrl,
                autoQualityLabel = autoQualityLabel,
            )

            ExtractedStream(
                url = qualities.values.last(),
                headers = headers,
                qualities = qualities,
            )
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            analyticsTracker.logExtractorFailure(
                "Rutube",
                normalizedUrl,
                "unexpected extractor error",
                e
            )
            null
        }
    }

    private suspend fun buildQualityMap(
        streamUrl: String,
        referer: String,
        autoQualityLabel: String,
    ): LinkedHashMap<String, String> {
        val candidates = linkedMapOf("auto" to streamUrl)
        val masterPlaylist = runSuspendCatching {
            httpClient.fetchText(
                url = streamUrl,
                headers = fetchHeaders(referer),
                throwOnFailure = true
            )
        }.getOrElse {
            analyticsTracker.logExtractorFailure(
                "Rutube",
                streamUrl,
                "failed to load master playlist, fallback to auto",
                it
            )
            return candidates.withAutoQualityLabel(autoQualityLabel)
        }

        var pendingQuality: String? = null
        masterPlaylist.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                if (line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                    pendingQuality = qualityLabelFromStreamInfo(line)
                    return@forEach
                }

                val quality = pendingQuality
                if (quality != null && !line.startsWith("#")) {
                    val variantUrl = normalizeUrl(line, streamUrl)
                    if (variantUrl.isNotBlank() && !candidates.containsKey(quality)) {
                        candidates[quality] = variantUrl
                    }
                    pendingQuality = null
                }
            }

        return orderQualityMap(candidates)
            .withAutoQualityLabel(autoQualityLabel)
    }

    private fun qualityLabelFromStreamInfo(line: String): String? {
        val height = RESOLUTION_PATTERN.find(line)
            ?.groupValues
            ?.getOrNull(2)
            ?.toIntOrNull()
            ?: return null

        return when (height) {
            144, 240, 360, 480, 720, 1080, 1440, 2160 -> "${height}p"
            else -> null
        }
    }

    private fun extractVideoId(url: String): String? {
        val segments = runCatching {
            URL(url).path
                .trim('/')
                .split('/')
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())

        val pathId = segments
            .firstOrNull { VIDEO_ID_PATTERN.matches(it) }
        if (!pathId.isNullOrBlank()) return pathId

        return VIDEO_ID_PATTERN.find(url)?.groupValues?.getOrNull(1)
    }

    private fun normalizeUrl(url: String, baseUrl: String = ""): String {
        val trimmed = url.trim().trim('"').trim('\'')
        if (trimmed.isBlank()) return ""

        return when {
            trimmed.hasKnownUrlScheme() -> normalizeUrlScheme(trimmed)
            trimmed.startsWith("/") -> "$RUTUBE_ORIGIN$trimmed"
            else -> resolveRelativeUrl(trimmed, baseUrl) { "https://$trimmed" }
        }
    }

    private fun streamHeaders(referer: String): Map<String, String> = mapOf(
        "Referer" to referer,
        "Origin" to RUTUBE_ORIGIN,
        "User-Agent" to CHROME_UA,
    )

    private fun fetchHeaders(referer: String): Map<String, String> =
        streamHeaders(referer) + ("Accept" to "*/*")
}

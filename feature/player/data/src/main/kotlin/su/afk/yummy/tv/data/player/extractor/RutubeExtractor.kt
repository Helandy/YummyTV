package su.afk.yummy.tv.data.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import su.afk.yummy.tv.domain.player.isRutubePlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import javax.inject.Inject

internal data class RutubeResult(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>? = null,
)

internal class RutubeExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
) : PlayerStreamExtractor {

    private val RUTUBE_ORIGIN = "https://rutube.ru"
    private val OPTIONS_URL = "$RUTUBE_ORIGIN/api/play/options/%s/?no_404=true"
    private val QUALITY_KEYS =
        listOf("auto", "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")
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
        )?.toStream() ?: PlayerStreamResolveResult.Failed

    private suspend fun extractStream(
        iframeUrl: String,
        autoQualityLabel: String = "auto"
    ): RutubeResult? = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(iframeUrl)

        try {
            val videoId = extractVideoId(normalizedUrl) ?: run {
                logExtractorFailure("Rutube", normalizedUrl, "video id not found")
                return@withContext null
            }
            val headers = streamHeaders(normalizedUrl)
            val options = JSONObject(fetchText(OPTIONS_URL.format(videoId), normalizedUrl))
            val streamUrl = options.optJSONObject("video_balancer")
                ?.let { balancer ->
                    balancer.optString("m3u8")
                        .ifBlank { balancer.optString("default") }
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                }
                ?: run {
                    logExtractorFailure("Rutube", normalizedUrl, "video_balancer stream not found")
                    return@withContext null
                }

            val qualities = buildQualityMap(
                streamUrl = streamUrl,
                referer = normalizedUrl,
                autoQualityLabel = autoQualityLabel,
            )

            RutubeResult(
                url = qualities.values.last(),
                headers = headers,
                qualities = qualities,
            )
        } catch (e: Exception) {
            logExtractorFailure("Rutube", normalizedUrl, "unexpected extractor error", e)
            null
        }
    }

    private suspend fun buildQualityMap(
        streamUrl: String,
        referer: String,
        autoQualityLabel: String,
    ): LinkedHashMap<String, String> {
        val candidates = linkedMapOf("auto" to streamUrl)
        val masterPlaylist = runCatching { fetchText(streamUrl, referer) }
            .getOrElse {
                logExtractorFailure(
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

    private fun orderQualityMap(raw: LinkedHashMap<String, String>): LinkedHashMap<String, String> {
        val ordered = LinkedHashMap<String, String>()
        QUALITY_KEYS.forEach { qualityKey ->
            raw[qualityKey]?.let { ordered[qualityKey] = it }
        }
        raw.forEach { (key, value) ->
            if (!ordered.containsKey(key)) ordered[key] = value
        }
        return ordered
    }

    private fun LinkedHashMap<String, String>.withAutoQualityLabel(
        autoQualityLabel: String
    ): LinkedHashMap<String, String> {
        if (autoQualityLabel.isBlank() || autoQualityLabel == "auto") return this

        return entries.associateTo(LinkedHashMap()) { (quality, url) ->
            if (quality == "auto") autoQualityLabel to url else quality to url
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
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "https://")
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("/") -> "$RUTUBE_ORIGIN$trimmed"
            baseUrl.isNotBlank() -> runCatching { URL(URL(baseUrl), trimmed).toString() }
                .getOrElse { "https://$trimmed" }

            else -> "https://$trimmed"
        }
    }

    private fun streamHeaders(referer: String): Map<String, String> = mapOf(
        "Referer" to referer,
        "Origin" to RUTUBE_ORIGIN,
        "User-Agent" to CHROME_UA,
    )

    private suspend fun fetchText(url: String, referer: String): String {
        val response = httpClient.getText(
            url = url,
            headers = mapOf(
                "Referer" to referer,
                "Origin" to RUTUBE_ORIGIN,
                "User-Agent" to CHROME_UA,
                "Accept" to "*/*",
            ),
        )
        if (!response.isSuccess) {
            throw IllegalStateException("HTTP ${response.statusCode}: ${response.body.take(80)}")
        }
        return response.body
    }

    private fun RutubeResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )
}

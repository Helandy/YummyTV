package su.afk.yummy.tv.data.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import java.net.HttpURLConnection
import java.net.URL

internal data class RutubeResult(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>? = null,
)

internal object RutubeExtractor {

    private const val RUTUBE_ORIGIN = "https://rutube.ru"
    private const val OPTIONS_URL = "$RUTUBE_ORIGIN/api/play/options/%s/?no_404=true"
    private const val HTTP_OK_START = 200
    private const val HTTP_OK_END = 299

    private val QUALITY_KEYS =
        listOf("auto", "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")
    private val VIDEO_ID_PATTERN = Regex("(?i)([a-f0-9]{32})")
    private val RESOLUTION_PATTERN = Regex("(?i)RESOLUTION=(\\d+)x(\\d+)")

    suspend fun extract(
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

    private fun buildQualityMap(
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

    private fun fetchText(url: String, referer: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Referer", referer)
            conn.setRequestProperty("Origin", RUTUBE_ORIGIN)
            conn.setRequestProperty("User-Agent", CHROME_UA)
            conn.setRequestProperty("Accept", "*/*")
            val responseCode = conn.responseCode
            if (responseCode !in HTTP_OK_START..HTTP_OK_END) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("HTTP $responseCode: ${error.take(80)}")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

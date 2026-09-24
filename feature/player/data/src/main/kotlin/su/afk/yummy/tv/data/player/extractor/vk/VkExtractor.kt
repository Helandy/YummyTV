package su.afk.yummy.tv.data.player.extractor.vk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.ExtractedStream
import su.afk.yummy.tv.data.player.extractor.common.decodeUnicodeEscapes
import su.afk.yummy.tv.data.player.extractor.common.fetchText
import su.afk.yummy.tv.data.player.extractor.common.hasKnownUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.data.player.extractor.common.normalizeUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.orderQualityMap
import su.afk.yummy.tv.data.player.extractor.common.resolveRelativeUrl
import su.afk.yummy.tv.data.player.extractor.common.withAutoQualityLabel
import su.afk.yummy.tv.data.player.network.CHROME_UA
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.domain.player.isVkPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import java.net.URLDecoder
import javax.inject.Inject

internal class VkExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
    private val analyticsTracker: AnalyticsTracker,
) : PlayerStreamExtractor {

    private val DEFAULT_REFERER = "https://vk.com/"
    private val YUMMY_ORIGIN = "https://ru.yummyani.me"
    private val VIDEO_EXT_URL = "https://vk.com/video_ext.php"
    private val VK_ID_DELIMITER = "_"

    private val NAMED_STREAM_PATTERNS = listOf(
        "1080p" to Regex("(?i)\\b(?:url1080|mp4_1080)\\b\\s*[:=]\\s*['\"]([^'\"]+)['\"]"),
        "720p" to Regex("(?i)\\b(?:url720|mp4_720)\\b\\s*[:=]\\s*['\"]([^'\"]+)['\"]"),
        "480p" to Regex("(?i)\\b(?:url480|mp4_480)\\b\\s*[:=]\\s*['\"]([^'\"]+)['\"]"),
        "360p" to Regex("(?i)\\b(?:url360|mp4_360)\\b\\s*[:=]\\s*['\"]([^'\"]+)['\"]"),
        "240p" to Regex("(?i)\\b(?:url240|mp4_240)\\b\\s*[:=]\\s*['\"]([^'\"]+)['\"]"),
        "auto" to Regex("(?i)\\burl\\b\\s*[:=]\\s*['\"]([^'\"]+)['\"]"),
        "auto" to Regex("(?i)\\\"hls_fmp4\\\"\\s*:\\s*['\"]([^'\"]+)['\"]"),
        "auto" to Regex("(?i)\\\"hls\\\"\\s*:\\s*['\"]([^'\"]+)['\"]"),
    )

    private val VK_FILE_QUALITY_PATTERNS = listOf(
        Regex("(?i)\\\"mp4_(\\d{3,4})\\\"\\s*:\\s*['\"]([^'\"]+)['\"]"),
    )

    private val FILES_BLOCK_PATTERN = Regex(
        "(?is)\\\"files\\\"\\s*:\\s*\\{(.*?)\\}\\s*,\\s*\\\"trailer\\\"",
    )

    private val VIDEO_PATH_ID_PATTERN = Regex("(?i)video(?<owner>-?\\d+)_(?<id>\\d+)")

    private val QUALITY_FROM_URL = Regex("(?i)(\\d{3,4})(?=p?(?:\\.mp4|/|$))")
    private val STREAM_URL_PATTERNS = listOf(
        Regex("(?i)https?:\\/\\/[^\"'\\s]+\\.(?:m3u8|mp4)[^\"'\\s]*"),
        Regex("(?i)\\/\\/[^\"'\\s]+\\.(?:m3u8|mp4)[^\"'\\s]*"),
        Regex("(?i)\\b(?:videoUrl|fileList|file|src)\\b[^=]*=\\s*['\"]([^'\"]+\\.(?:m3u8|mp4)[^'\"]*)['\"]"),
    )
    private val DATA_SRC_PATTERNS = listOf(
        Regex("(?i)data-video(?:-src|Src)\\s*=\\s*['\"]([^'\"]+)['\"]"),
        Regex("(?i)<source[^>]+src=['\"]([^'\"]+\\.(?:m3u8|mp4)[^'\"]*)['\"]"),
    )

    override fun supports(url: String): Boolean = url.isVkPlayerUrl()

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
        val referer = normalizedUrl.ifBlank { DEFAULT_REFERER }

        try {
            val iframeHtml = normalizePayload(fetchPageText(normalizedUrl, referer))
            val videoExtUrl = resolveVideoExtUrl(iframeHtml, normalizedUrl)
            val sourceHtml = videoExtUrl?.let {
                val source = it
                runSuspendCatching { normalizePayload(fetchPageText(source, normalizedUrl)) }
                    .getOrElse {
                        analyticsTracker.logExtractorFailure(
                            "VK",
                            source,
                            "failed to load video_ext page, fallback to iframe",
                            it
                        )
                        iframeHtml
                    }
            } ?: iframeHtml

            val sourceUrl = videoExtUrl ?: normalizedUrl
            val candidates = collectCandidates(sourceHtml, sourceUrl)
            if (candidates.isEmpty()) {
                analyticsTracker.logExtractorFailure("VK", normalizedUrl, "no stream URLs found")
                return@withContext null
            }

            val qualities = orderQualityMap(
                raw = candidates,
                keyAliases = { key ->
                    if (key == "auto") listOf(key) else listOf(
                        key,
                        key.removeSuffix("p")
                    )
                },
            ).withAutoQualityLabel(autoQualityLabel)

            ExtractedStream(
                url = qualities.values.last(),
                headers = streamHeaders(sourceUrl),
                qualities = qualities,
            )
        } catch (e: Exception) {
            analyticsTracker.logExtractorFailure(
                "VK",
                normalizedUrl,
                "unexpected extractor error",
                e
            )
            null
        }
    }

    private fun collectCandidates(html: String, baseUrl: String): LinkedHashMap<String, String> {
        val candidates = LinkedHashMap<String, String>()

        for ((quality, pattern) in NAMED_STREAM_PATTERNS) {
            pattern.findAll(html).forEach { match ->
                val rawUrl = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val normalized = normalizeUrl(rawUrl, baseUrl)
                addCandidate(candidates, quality, normalized)
            }
        }

        VK_FILE_QUALITY_PATTERNS.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val qualityValue = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val rawUrl = match.groupValues.getOrNull(2)?.trim().orEmpty()
                val normalized = normalizeUrl(rawUrl, baseUrl)
                val quality = if (qualityValue.isBlank()) "auto" else "${qualityValue}p"
                addCandidate(candidates, quality, normalized)
            }
        }

        FILES_BLOCK_PATTERN.find(html)?.groupValues?.getOrNull(1)?.let { filesBlock ->
            val block = filesBlock
                .replace("\"trailer\"", "")
            FILES_KEY_VALUE_PATTERNS.forEach { pattern ->
                pattern.findAll(block).forEach { match ->
                    val quality =
                        normalizeQualityLabel(match.groupValues.getOrNull(1)?.trim().orEmpty())
                    val rawUrl = match.groupValues.getOrNull(2)?.trim().orEmpty()
                    val normalized = normalizeUrl(rawUrl, baseUrl)
                    addCandidate(candidates, quality, normalized)
                }
            }
        }

        STREAM_URL_PATTERNS.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val raw = match.groupValues.getOrNull(1)?.trim() ?: match.value
                val normalized = normalizeUrl(raw, baseUrl)
                val quality = normalizeQualityFromUrl(raw)
                addCandidate(candidates, quality, normalized)
            }
        }

        // fallback for src attributes if previous regex misses full values
        val plainSrcPattern = Regex("(?i)<source[^>]+src=['\"]([^'\"]+)['\"]")
        plainSrcPattern.findAll(html).forEach { match ->
            val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val normalized = normalizeUrl(raw, baseUrl)
            val quality = normalizeQualityFromUrl(raw)
            addCandidate(candidates, quality, normalized)
        }

        DATA_SRC_PATTERNS.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val normalized = normalizeUrl(raw, baseUrl)
                val quality = normalizeQualityFromUrl(raw)
                addCandidate(candidates, quality, normalized)
            }
        }

        return candidates
    }

    private val FILES_KEY_VALUE_PATTERNS = listOf(
        Regex("(?i)\\\"(mp4_\\d{3,4}|hls_fmp4|hls|url\\d{3,4}|url)\\\"\\s*:\\s*['\"]([^'\"]+)['\"]"),
    )

    private fun addCandidate(
        candidates: LinkedHashMap<String, String>,
        quality: String,
        url: String
    ) {
        if (url.isBlank() || !isStreamLike(url, quality)) return
        val cleaned = normalizeEscapedUrl(url)
        if (candidates.any { (key, value) -> key != quality && value == cleaned }) return

        val current = candidates[quality]
        if (current == null || current.length < cleaned.length) {
            candidates[quality] = cleaned
        }
    }

    private fun normalizeQualityFromUrl(url: String): String {
        val found = QUALITY_FROM_URL.find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return "auto"

        return when (found) {
            144, 240, 360, 480, 720, 1080, 1440, 2160 -> "${found}p"
            else -> "auto"
        }
    }

    private fun normalizeQualityLabel(label: String): String {
        val normalized = label.trim().lowercase()
        if (normalized.isBlank()) return "auto"
        if (normalized == "hls" || normalized == "hls_fmp4" || normalized == "url") return "auto"

        val quality = Regex("(\\d{3,4})").find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return when (quality) {
            144, 240, 360, 480, 720, 1080, 1440, 2160 -> "${quality}p"
            else -> label
        }
    }

    private fun isStreamLike(url: String, quality: String): Boolean {
        val lowered = url.lowercase()
        if (lowered.contains(".m3u8") || lowered.contains(".mp4")) return true

        val isKnownVkCdn = lowered.contains("okcdn.ru") ||
                lowered.contains("vkuser") ||
                lowered.contains("userapi.com") ||
                lowered.contains("vkvd")
        val isNamedVideoQuality = quality != "auto" && isHttpUrl(url)
        return isKnownVkCdn && isNamedVideoQuality
    }

    private fun resolveVideoExtUrl(pageHtml: String, iframeUrl: String): String? {
        val normalized = normalizeUrl(iframeUrl)
        if (normalized.contains("video_ext.php", ignoreCase = true)) {
            return normalized
        }

        parseVideoPairFromText(normalized)?.let { (ownerId, videoId) ->
            return "$VIDEO_EXT_URL?oid=$ownerId&id=$videoId&hd=1"
        }

        parseQueryParam(normalized, "id")?.let { id ->
            parseVideoPairFromId(id)?.let { (ownerId, videoId) ->
                return "$VIDEO_EXT_URL?oid=$ownerId&id=$videoId&hd=1"
            }
        }

        run {
            val match = VIDEO_PATH_ID_PATTERN.find(pageHtml)
            val ownerId = match?.groups?.get("owner")?.value
            val videoId = match?.groups?.get("id")?.value
            if (!ownerId.isNullOrBlank() && !videoId.isNullOrBlank()) {
                return "$VIDEO_EXT_URL?oid=$ownerId&id=$videoId&hd=1"
            }
        }

        return VIDEO_EXT_FROM_IFRAME_PATTERNS.firstNotNullOfOrNull { pattern ->
            val match = pattern.find(pageHtml) ?: return@firstNotNullOfOrNull null
            val ownerId = match.groupValues.getOrNull(1).orEmpty()
            val videoId = match.groupValues.getOrNull(2).orEmpty()
            if (ownerId.isBlank() || videoId.isBlank()) null
            else "$VIDEO_EXT_URL?oid=$ownerId&id=$videoId&hd=1"
        }
    }

    private fun parseVideoPairFromId(rawId: String): Pair<String, String>? {
        val id = rawId.removePrefix("video")
        if (!id.contains(VK_ID_DELIMITER)) return null
        val splitIndex = id.indexOf(VK_ID_DELIMITER)
        if (splitIndex <= 0 || splitIndex >= id.length - 1) return null
        val ownerId = id.substring(0, splitIndex)
        val videoId = id.substring(splitIndex + 1)
        return ownerId.takeIf { it.isNotBlank() }?.let { owner ->
            videoId.takeIf { it.isNotBlank() }?.let { owner to it }
        }
    }

    private fun parseVideoPairFromText(text: String): Pair<String, String>? {
        val match = VIDEO_PATH_ID_PATTERN.find(text) ?: return null
        val ownerId = match.groups["owner"]?.value.orEmpty()
        val videoId = match.groups["id"]?.value.orEmpty()
        return ownerId.takeIf { it.isNotBlank() }?.let { owner ->
            videoId.takeIf { it.isNotBlank() }?.let { owner to it }
        }
    }

    private fun parseQueryParam(url: String, key: String): String? {
        val query = url.substringAfter("?", "").substringBefore("#")
        if (query.isBlank()) return null

        return query
            .split("&")
            .firstNotNullOfOrNull {
                val idx = it.indexOf('=')
                if (idx < 0) return@firstNotNullOfOrNull null
                val param = it.substring(0, idx)
                if (!param.equals(key, ignoreCase = true)) return@firstNotNullOfOrNull null
                val value = it.substring(idx + 1)
                runCatching { URLDecoder.decode(value, "UTF-8") }.getOrNull() ?: value
            }
    }

    private fun normalizePayload(text: String): String =
        decodeUnicodeEscapes(
            text.replace("\\/", "/").replace("\\u002f", "/").replace("\\u002F", "/")
        )

    private fun normalizeUrl(url: String, baseUrl: String = ""): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""

        val normalized = normalizePayload(trimmed)
            .trim()
            .trim('"')
            .trim('\'')

        if (normalized.isBlank()) return ""

        return when {
            normalized.hasKnownUrlScheme() -> normalizeUrlScheme(normalized)
            normalized.startsWith("/") -> {
                val origin = runCatching {
                    val base = URL(baseUrl)
                    "${base.protocol}://${base.host}"
                }.getOrNull() ?: YUMMY_ORIGIN
                "$origin$normalized"
            }

            else -> resolveRelativeUrl(normalized, baseUrl) { "https://$normalized" }
        }
    }

    private fun normalizeEscapedUrl(url: String): String =
        decodeUnicodeEscapes(url)
            .replace("\\u0026", "&")
            .replace("\\u002D", "-")
            .replace("\\u002d", "-")
            .replace("\\/", "/")

    private fun streamHeaders(referer: String): Map<String, String> = mapOf(
        "Referer" to referer,
        "Origin" to DEFAULT_REFERER.removeSuffix("/"),
        "User-Agent" to CHROME_UA,
    )

    private fun isHttpUrl(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith(
            "http://",
            ignoreCase = true
        )

    private suspend fun fetchPageText(url: String, referer: String): String =
        httpClient.fetchText(
            url = url,
            headers = mapOf(
                "Referer" to referer,
                "User-Agent" to CHROME_UA,
                "Accept" to "*/*",
            ),
            throwOnFailure = true,
        )

    private val VIDEO_EXT_FROM_IFRAME_PATTERNS = listOf(
        Regex("video_ext\\.php[^\"']*?oid=([^&\"']+)&id=([^&\"']+)", RegexOption.IGNORE_CASE),
    )
}

package su.afk.yummy.tv.feature.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.feature.player.utils.CHROME_UA
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

internal data class VkResult(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>? = null,
)

internal object VkExtractor {

    private const val DEFAULT_REFERER = "https://vk.com/"
    private const val YUMMY_ORIGIN = "https://ru.yummyani.me"
    private const val VIDEO_EXT_URL = "https://vk.com/video_ext.php"
    private const val HTTP_OK_START = 200
    private const val HTTP_OK_END = 299
    private const val VK_ID_DELIMITER = "_"

    private val QUALITY_KEYS =
        listOf("auto", "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")

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

    suspend fun extract(
        iframeUrl: String,
        autoQualityLabel: String = "auto"
    ): VkResult? = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(iframeUrl)
        val referer = normalizedUrl.ifBlank { DEFAULT_REFERER }

        try {
            val iframeHtml = normalizePayload(fetchText(normalizedUrl, referer))
            val videoExtUrl = resolveVideoExtUrl(iframeHtml, normalizedUrl)
            val sourceHtml = videoExtUrl?.let {
                val source = it
                runCatching { normalizePayload(fetchText(source, normalizedUrl)) }
                    .getOrElse {
                        logExtractorFailure(
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
                logExtractorFailure("VK", normalizedUrl, "no stream URLs found")
                return@withContext null
            }

            val qualities = orderQualityMap(candidates)
                .withAutoQualityLabel(autoQualityLabel)

            VkResult(
                url = qualities.values.last(),
                headers = streamHeaders(sourceUrl),
                qualities = qualities,
            )
        } catch (e: Exception) {
            logExtractorFailure("VK", normalizedUrl, "unexpected extractor error", e)
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

    private fun orderQualityMap(raw: LinkedHashMap<String, String>): LinkedHashMap<String, String> {
        val ordered = LinkedHashMap<String, String>()
        QUALITY_KEYS.forEach { qualityKey ->
            raw[qualityKey]?.let { ordered[qualityKey] = it }
            if (qualityKey == "auto") return@forEach
            if (!ordered.containsKey(qualityKey)) {
                raw[qualityKey.removeSuffix("p")]?.let { ordered[qualityKey] = it }
            }
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

    private fun decodeUnicodeEscapes(text: String): String {
        if (!text.contains("\\u")) return text

        val output = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 5 < text.length && text[i + 1] == 'u') {
                val hex = text.substring(i + 2, i + 6)
                runCatching {
                    output.append(hex.toInt(16).toChar())
                    i += 6
                }.getOrElse {
                    output.append(c)
                    i++
                }
            } else {
                output.append(c)
                i++
            }
        }
        return output.toString()
            .replace("\\u0026", "&")
            .replace("\\u002D", "-")
            .replace("\\u002d", "-")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    }

    private fun normalizeUrl(url: String, baseUrl: String = ""): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""

        val normalized = normalizePayload(trimmed)
            .trim()
            .trim('"')
            .trim('\'')

        if (normalized.isBlank()) return ""

        return when {
            normalized.startsWith("//") -> "https:$normalized"
            normalized.startsWith("http://") -> normalized.replaceFirst("http://", "https://")
            normalized.startsWith("https://") -> normalized
            normalized.startsWith("/") -> {
                val origin = runCatching {
                    val base = URL(baseUrl)
                    "${base.protocol}://${base.host}"
                }.getOrNull() ?: YUMMY_ORIGIN
                "$origin$normalized"
            }

            baseUrl.isNotBlank() -> runCatching {
                URL(baseUrl).let { base ->
                    URL(base, normalized).toString()
                }
            }.getOrElse { "https://$normalized" }

            else -> "https://$normalized"
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

    private fun fetchText(url: String, referer: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Referer", referer)
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

    private val VIDEO_EXT_FROM_IFRAME_PATTERNS = listOf(
        Regex("video_ext\\.php[^\"']*?oid=([^&\"']+)&id=([^&\"']+)", RegexOption.IGNORE_CASE),
    )
}

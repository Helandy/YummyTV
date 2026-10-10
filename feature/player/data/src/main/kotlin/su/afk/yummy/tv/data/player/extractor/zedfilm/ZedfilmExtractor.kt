package su.afk.yummy.tv.data.player.extractor.zedfilm

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.core.utils.network.RU_ACCEPT_LANGUAGE
import su.afk.yummy.tv.core.utils.player.isZedfilmPlayerUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.ExtractedStream
import su.afk.yummy.tv.data.player.extractor.common.hasKnownUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.data.player.extractor.common.normalizeUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.orderQualityMap
import su.afk.yummy.tv.data.player.extractor.common.resolveRelativeUrl
import su.afk.yummy.tv.data.player.extractor.common.withAutoQualityLabel
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.nio.charset.Charset
import javax.inject.Inject

internal class ZedfilmExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
    private val analyticsTracker: AnalyticsTracker,
    private val userAgents: BrowserUserAgentProvider,
) : PlayerStreamExtractor {

    private val ZEDFILM_ORIGIN = "https://zedfilm.ru"
    private val HLAMER_ORIGIN = "https://hlamer.ru"
    private val YANI_REFERER = "https://yani.tv/"

    private val STREAM_URL_PATTERNS = listOf(
        Regex("(?i)https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:mpd|m3u8|mp4)[^\"'\\s<>]*"),
        Regex("(?i)//[^\"'\\s<>]+\\.(?:mpd|m3u8|mp4)[^\"'\\s<>]*"),
        Regex("(?i)\\b(?:file|src|source|url|hls|dash)\\b\\s*[:=]\\s*['\"]([^'\"]+\\.(?:mpd|m3u8|mp4)[^'\"]*)['\"]"),
        Regex("(?i)<source[^>]+src=['\"]([^'\"]+\\.(?:mpd|m3u8|mp4)[^'\"]*)['\"]"),
    )

    // Zedfilm embeds stream metadata as: video_Init('eyJ2X2lkIj...')
    // The Base64 payload is JSON with fields such as url, url2, dash, type, and tracks.
    private val VIDEO_INIT_PATTERN = Regex("""(?i)video_Init\(\s*['"]([^'"]+)['"]""")
    // Длинные значения первыми и `(?!\d)` после числа: иначе `1440` читалось бы как `144`.
    private val QUALITY_FROM_TEXT = Regex("(?i)(?<!\\d)(2160|1440|1080|720|480|360|240|144)(?!\\d)p?")
    private val URL_ORIGIN_PATTERN = Regex("^(?:https?:)?//[^/?#]+")
    private val AD_URL_TOKENS = setOf("ads", "ima")
    private val AD_HOST_MARKERS = listOf("doubleclick", "yandex")

    /**
     * GET iframe HTML, decode video_Init(Base64 JSON), use url as DASH .mpd and url2 as MP4
     * fallback, then scan the page for stream URLs. There is no WebView fallback: if the page
     * contract changes this reports a failure (logged as "no stream URLs found in iframe page").
     */
    override fun supports(url: String): Boolean = url.isZedfilmPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult =
        withContext(Dispatchers.IO) {
            extractStatic(normalizeUrl(request.iframeUrl), request.autoQualityLabel)
        }?.toResolveResult() ?: PlayerStreamResolveResult.Failed("Zedfilm: no stream found")

    private suspend fun extractStatic(
        playerUrl: String,
        autoQualityLabel: String,
    ): ExtractedStream? {
        val html = runSuspendCatching { fetchIframeHtml(playerUrl) }
            .getOrElse {
                analyticsTracker.logExtractorFailure(
                    "Zedfilm",
                    playerUrl,
                    "failed to load iframe page",
                    it
                )
                return null
            }
        val candidates = collectCandidates(html, playerUrl)
        if (candidates.isEmpty()) {
            analyticsTracker.logExtractorFailure(
                "Zedfilm",
                playerUrl,
                "no stream URLs found in iframe page"
            )
            return null
        }

        val qualities = orderQualityMap(candidates)
            .withAutoQualityLabel(autoQualityLabel)
        return ExtractedStream(
            url = qualities.values.last(),
            headers = streamHeaders(playerUrl),
            qualities = qualities.takeIf { it.size > 1 },
        )
    }

    private fun collectCandidates(html: String, baseUrl: String): LinkedHashMap<String, String> {
        val candidates = LinkedHashMap<String, String>()
        val payload = normalizePayload(html)
        collectVideoInitCandidates(payload, baseUrl).forEach { (quality, url) ->
            candidates[quality] = url
        }
        STREAM_URL_PATTERNS.forEach { pattern ->
            pattern.findAll(payload).forEach { match ->
                val raw = match.groupValues.getOrNull(1)
                    ?.takeIf { it.isNotBlank() }
                    ?: match.value
                val url = normalizeEscapedUrl(normalizeUrl(raw, baseUrl))
                if (isStreamUrl(url)) {
                    candidates[qualityLabelFromText(raw)] = url
                }
            }
        }
        return candidates
    }

    private fun collectVideoInitCandidates(
        html: String,
        baseUrl: String,
    ): LinkedHashMap<String, String> {
        val encoded = VIDEO_INIT_PATTERN.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?: return linkedMapOf()

        val json = runCatching {
            String(Base64.decode(encoded, Base64.DEFAULT))
        }.getOrElse {
            analyticsTracker.logExtractorFailure(
                "Zedfilm",
                baseUrl,
                "failed to decode video_Init payload",
                it
            )
            return linkedMapOf()
        }
        val data = runCatching { JSONObject(json) }.getOrElse {
            analyticsTracker.logExtractorFailure(
                "Zedfilm",
                baseUrl,
                "failed to parse video_Init payload",
                it
            )
            return linkedMapOf()
        }

        // This matches Zedfilm's own player code: url is the primary DASH stream, url2 is MP4 fallback.
        val primaryUrl = data.optString("url")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?.let { normalizeUrl(it, baseUrl) }
            ?.takeIf(::isStreamUrl)
        val fallbackUrl = data.optString("url2")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?.let { normalizeUrl(it, baseUrl) }
            ?.takeIf(::isStreamUrl)
        val streamUrl = primaryUrl ?: fallbackUrl

        return streamUrl
            ?.let { linkedMapOf(qualityLabelFromText(it) to it) }
            ?: linkedMapOf()
    }

    private fun isStreamUrl(url: String): Boolean {
        val lowered = url.lowercase()
        val isMedia = lowered.contains(".mpd") || lowered.contains(".m3u8") || lowered.contains(".mp4")
        if (!isMedia) return false
        if (AD_HOST_MARKERS.any(lowered::contains)) return false
        // Рекламные маркеры ищем целыми словами: подстрока «ads»/«ima» режет `uploads`,
        // `animation` и `prima`, то есть вполне нормальные ссылки.
        return lowered.split(Regex("[^a-z0-9]+")).none { it in AD_URL_TOKENS }
    }

    // Хост отбрасываем: `s240.cdn.example` — это не качество.
    private fun qualityLabelFromText(text: String): String =
        QUALITY_FROM_TEXT.find(text.replace(URL_ORIGIN_PATTERN, ""))
            ?.groupValues
            ?.getOrNull(1)
            ?.let { "${it}p" }
            ?: "auto"

    private fun normalizePayload(payload: String): String =
        payload
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("\\u0026", "&")

    private fun normalizeEscapedUrl(url: String): String =
        normalizePayload(url).trim().trim('"').trim('\'')

    private fun normalizeUrl(url: String, baseUrl: String = ""): String {
        val trimmed = normalizeEscapedUrl(url)
        if (trimmed.isBlank()) return ""

        return when {
            trimmed.hasKnownUrlScheme() -> normalizeUrlScheme(trimmed)
            trimmed.startsWith("/") -> "$HLAMER_ORIGIN$trimmed"
            else -> resolveRelativeUrl(trimmed, baseUrl) { "$ZEDFILM_ORIGIN/$trimmed" }
        }
    }

    private fun streamHeaders(referer: String): Map<String, String> = mapOf(
        "Referer" to referer,
        "Origin" to HLAMER_ORIGIN,
        "User-Agent" to userAgents.userAgent,
    )

    // Zedfilm serves cyrillic error/meta text in windows-1251; the shared fetchText() helper
    // always decodes as UTF-8, so this stays a direct call to control the charset.
    //
    // Accept-Language and Accept-Encoding are explicit on purpose: without both the site answers
    // the iframe URL with 404 "video not found". gzip is safe here only because the injected
    // client has the ContentEncoding plugin (NetworkModule.provideHttpClient), which decodes the
    // body even when the header is set by the caller. Neither header may go into stream headers:
    // those are replayed by Media3, which sends `Accept-Encoding: identity` for media.
    private suspend fun fetchIframeHtml(url: String): String =
        httpClient.getText(
            url = url,
            headers = mapOf(
                "Referer" to YANI_REFERER,
                "Origin" to HLAMER_ORIGIN,
                "User-Agent" to userAgents.userAgent,
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to RU_ACCEPT_LANGUAGE,
                "Accept-Encoding" to "gzip",
            ),
        ).body(Charset.forName("windows-1251"))
}

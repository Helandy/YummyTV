package su.afk.yummy.tv.data.player.extractor

import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import su.afk.yummy.tv.data.player.utils.withBrowserUserAgent
import su.afk.yummy.tv.domain.player.isZedfilmPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.coroutines.resume

internal data class ZedfilmResult(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>? = null,
)

internal class ZedfilmExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
) : PlayerStreamExtractor {

    private val ZEDFILM_ORIGIN = "https://zedfilm.ru"
    private val HLAMER_ORIGIN = "https://hlamer.ru"
    private val YANI_REFERER = "https://yani.tv/"
    private val TIMEOUT_MS = 20_000L
    private val STREAM_SETTLE_DELAY_MS = 2_000L

    private val QUALITY_KEYS =
        listOf("auto", "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")
    private val STREAM_URL_PATTERNS = listOf(
        Regex("(?i)https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:mpd|m3u8|mp4)[^\"'\\s<>]*"),
        Regex("(?i)//[^\"'\\s<>]+\\.(?:mpd|m3u8|mp4)[^\"'\\s<>]*"),
        Regex("(?i)\\b(?:file|src|source|url|hls|dash)\\b\\s*[:=]\\s*['\"]([^'\"]+\\.(?:mpd|m3u8|mp4)[^'\"]*)['\"]"),
        Regex("(?i)<source[^>]+src=['\"]([^'\"]+\\.(?:mpd|m3u8|mp4)[^'\"]*)['\"]"),
    )

    // Zedfilm embeds stream metadata as: video_Init('eyJ2X2lkIj...')
    // The Base64 payload is JSON with fields such as url, url2, dash, type, and tracks.
    private val VIDEO_INIT_PATTERN = Regex("""(?i)video_Init\(\s*['"]([^'"]+)['"]""")
    private val QUALITY_FROM_TEXT = Regex("(?i)(?<!\\d)(144|240|360|480|720|1080|1440|2160)p?")

    /**
     * Main flow: GET iframe HTML, decode video_Init(Base64 JSON), use url as DASH .mpd and url2
     * as MP4 fallback. If that page contract changes, fall back to WebView request interception.
     */
    override fun supports(url: String): Boolean = url.isZedfilmPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult =
        extractStream(
            iframeUrl = request.iframeUrl,
            context = context,
            autoQualityLabel = request.autoQualityLabel,
        )?.toStream() ?: PlayerStreamResolveResult.Failed

    private suspend fun extractStream(
        iframeUrl: String,
        context: Context,
        autoQualityLabel: String = "auto",
    ): ZedfilmResult? {
        val playerUrl = normalizeUrl(iframeUrl)
        val staticResult = withContext(Dispatchers.IO) {
            extractStatic(playerUrl, autoQualityLabel)
        }
        return staticResult ?: withContext(Dispatchers.Main) {
            extractViaWebView(playerUrl, context, autoQualityLabel)
        }
    }

    private suspend fun extractStatic(
        playerUrl: String,
        autoQualityLabel: String,
    ): ZedfilmResult? {
        val html = runCatching { fetchText(playerUrl) }
            .getOrElse {
                logExtractorFailure("Zedfilm", playerUrl, "failed to load iframe page", it)
                return null
            }
        val candidates = collectCandidates(html, playerUrl)
        if (candidates.isEmpty()) {
            logExtractorFailure("Zedfilm", playerUrl, "no stream URLs found in iframe page")
            return null
        }

        val qualities = orderQualityMap(candidates)
            .withAutoQualityLabel(autoQualityLabel)
        return ZedfilmResult(
            url = qualities.values.last(),
            headers = streamHeaders(playerUrl),
            qualities = qualities.takeIf { it.size > 1 },
        )
    }

    /** Emergency fallback for changed/obfuscated pages; the normal Zedfilm path is static parsing. */
    private suspend fun extractViaWebView(
        playerUrl: String,
        context: Context,
        autoQualityLabel: String,
    ): ZedfilmResult? = suspendCancellableCoroutine { cont ->
        var webView: WebView? = null
        var delivered = false
        val handler = Handler(Looper.getMainLooper())
        val capturedStreams = LinkedHashMap<String, CapturedStream>()
        var fallbackStream: CapturedStream? = null
        var settleRunnable: Runnable? = null
        lateinit var timeoutRunnable: Runnable

        fun deliver(result: ZedfilmResult?) {
            if (delivered) return
            delivered = true
            val wv = webView
            webView = null
            settleRunnable?.let(handler::removeCallbacks)
            handler.removeCallbacks(timeoutRunnable)
            handler.post { wv?.destroy() }
            if (cont.isActive) cont.resume(result)
        }

        fun resultFromCapturedStreams(): ZedfilmResult? {
            val qualities = capturedStreams.toQualityMap()
                .withAutoQualityLabel(autoQualityLabel)
            val stream = qualities.values.lastOrNull()?.let { url ->
                capturedStreams.values.firstOrNull { it.url == url }
            } ?: fallbackStream

            return stream?.let {
                ZedfilmResult(
                    url = it.url,
                    headers = it.headers,
                    qualities = qualities.takeIf { qualityMap -> qualityMap.size > 1 },
                )
            }
        }

        fun scheduleDelivery(delayMs: Long = STREAM_SETTLE_DELAY_MS) {
            settleRunnable?.let(handler::removeCallbacks)
            val runnable = Runnable { deliver(resultFromCapturedStreams()) }
            settleRunnable = runnable
            handler.postDelayed(runnable, delayMs)
        }

        fun captureStream(url: String, requestHeaders: Map<String, String>) {
            val cleanedUrl = normalizeEscapedUrl(normalizeUrl(url, playerUrl))
            if (!isStreamUrl(cleanedUrl)) return

            val headers = (requestHeaders + streamHeaders(playerUrl)).withBrowserUserAgent()
            val stream = CapturedStream(url = cleanedUrl, headers = headers)
            fallbackStream = stream
            capturedStreams[qualityLabelFromText(cleanedUrl)] = stream
            scheduleDelivery()
        }

        timeoutRunnable = Runnable {
            if (resultFromCapturedStreams() == null) {
                logExtractorFailure(
                    "Zedfilm",
                    playerUrl,
                    "timed out before any stream was captured"
                )
            }
            deliver(resultFromCapturedStreams())
        }
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                allowFileAccess = false
                mediaPlaybackRequiresUserGesture = false
                userAgentString = CHROME_UA
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    captureStream(request.url.toString(), request.requestHeaders)
                    return null
                }

                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript(playProbeScript(), null)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    logExtractorFailure(
                        extractor = "Zedfilm",
                        url = request.url.toString(),
                        reason = "WebView error ${error.errorCode}: ${error.description}",
                    )
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    error: SslError,
                ) {
                    logExtractorFailure(
                        extractor = "Zedfilm",
                        url = error.url.orEmpty(),
                        reason = "WebView SSL error ${error.primaryError}",
                    )
                    super.onReceivedSslError(view, handler, error)
                }
            }

            loadUrl(playerUrl, streamHeaders(playerUrl))
        }

        cont.invokeOnCancellation {
            handler.removeCallbacks(timeoutRunnable)
            deliver(null)
        }
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
            logExtractorFailure("Zedfilm", baseUrl, "failed to decode video_Init payload", it)
            return linkedMapOf()
        }
        val data = runCatching { JSONObject(json) }.getOrElse {
            logExtractorFailure("Zedfilm", baseUrl, "failed to parse video_Init payload", it)
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

    private fun LinkedHashMap<String, CapturedStream>.toQualityMap(): LinkedHashMap<String, String> =
        orderQualityMap(entries.associateTo(LinkedHashMap()) { it.key to it.value.url })

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
        autoQualityLabel: String,
    ): LinkedHashMap<String, String> {
        if (autoQualityLabel.isBlank() || autoQualityLabel == "auto") return this

        return entries.associateTo(LinkedHashMap()) { (quality, url) ->
            if (quality == "auto") autoQualityLabel to url else quality to url
        }
    }

    private fun isStreamUrl(url: String): Boolean {
        val lowered = url.lowercase()
        if (!lowered.contains(".mpd") &&
            !lowered.contains(".m3u8") &&
            !lowered.contains(".mp4")
        ) return false
        return !lowered.contains("ima") &&
                !lowered.contains("doubleclick") &&
                !lowered.contains("ads") &&
                !lowered.contains("yandex")
    }

    private fun qualityLabelFromText(text: String): String =
        QUALITY_FROM_TEXT.find(text)
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
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "https://")
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("/") -> "$HLAMER_ORIGIN$trimmed"
            baseUrl.isNotBlank() -> runCatching { URL(URL(baseUrl), trimmed).toString() }
                .getOrElse { "$ZEDFILM_ORIGIN/$trimmed" }

            else -> "https://$trimmed"
        }
    }

    private fun streamHeaders(referer: String): Map<String, String> = mapOf(
        "Referer" to referer,
        "Origin" to HLAMER_ORIGIN,
        "User-Agent" to CHROME_UA,
    )

    private suspend fun fetchText(url: String): String =
        httpClient.getText(
            url = url,
            headers = mapOf(
                "Referer" to YANI_REFERER,
                "Origin" to HLAMER_ORIGIN,
                "User-Agent" to CHROME_UA,
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            ),
        ).body(Charset.forName("windows-1251"))

    private fun playProbeScript(): String = """
        (function(){
            try {
                var videos = Array.prototype.slice.call(document.querySelectorAll("video"));
                videos.forEach(function(video){
                    video.muted = true;
                    video.play().catch(function(){});
                });
            } catch(e) {}
        })();
    """.trimIndent()

    private data class CapturedStream(
        val url: String,
        val headers: Map<String, String>,
    )

    private fun ZedfilmResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )
}

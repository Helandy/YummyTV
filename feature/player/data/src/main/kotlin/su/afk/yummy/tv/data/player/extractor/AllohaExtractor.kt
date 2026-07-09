package su.afk.yummy.tv.data.player.extractor

import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.io.ByteArrayInputStream
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Stream data resolved from an Alloha iframe.
 *
 * @property url default stream URL used to start playback. When multiple qualities are captured, the
 * highest known quality is preferred.
 * @property headers request headers captured from the WebView HLS request. Alloha signs requests in
 * page JavaScript, so these headers must be reused by the player.
 * @property qualities optional map of quality labels (`360p`, `720p`) to their resolved HLS URLs.
 */
internal data class AllohaResult(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>? = null,
    val qualityHeaders: Map<String, Map<String, String>> = emptyMap(),
)

/**
 * Extracts playable HLS streams from Alloha iframe pages.
 *
 * Alloha renders a JS player inside an iframe and computes signed HLS requests in obfuscated page
 * code. Static parsing can see hints like `fileList`, but it cannot reproduce the signed request
 * headers reliably. For that reason this extractor loads the iframe in a hidden WebView, lets the
 * page create the player, and intercepts outgoing `.m3u8` requests.
 *
 * The embedded player used by Alloha exposes HLS qualities through Allplay-style fields such as
 * `player.config.quality.options` and `player.quality`, not only through PlayerJS APIs. The injected
 * probe collects known numeric qualities, switches them one by one, and lets
 * [shouldInterceptRequest][WebViewClient.shouldInterceptRequest] capture the resulting stream URL
 * plus headers for each quality.
 */
internal class AllohaExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
) : PlayerStreamExtractor {

    private val TIMEOUT_MS = 25_000L
    private val QUALITY_SWITCH_DELAY_MS = 1_500L
    private val STREAM_SETTLE_DELAY_MS = 2_500L
    private val KNOWN_QUALITY_LEVELS = setOf(240, 360, 480, 540, 720, 1080, 1440, 2160)
    private val QUALITY_IN_URL_REGEX = Regex("""(?<!\d)(\d{3,4})(?:p|\.mp4|/)""")

    override fun supports(url: String): Boolean = url.isAllohaPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult =
        extractStream(
            iframeUrl = request.iframeUrl,
            context = context,
            preferredQualityLabel = cleanQualityLabel(request.autoQualityLabel)
                ?.takeIf { qualityNumber(it) != null },
        )?.toStream() ?: PlayerStreamResolveResult.Failed

    private suspend fun extractStream(
        iframeUrl: String,
        context: Context,
        preferredQualityLabel: String?,
    ): AllohaResult? {
        // A new page URL and a clean Alloha origin force the embedded player to mint a new
        // authorization session. Reloading the same iframe URL otherwise reuses the old signed
        // session: the UI appears to refresh successfully, but its segments still become
        // `session_blocked` at the original session expiry.
        val fullUrl = normalizeUrl(iframeUrl).withSessionNonce()
        return withContext(Dispatchers.Main) {
            extractViaWebView(
                iframeUrl = fullUrl,
                context = context,
                preferredQualityLabel = preferredQualityLabel,
            )
        }
    }

    private suspend fun extractViaWebView(
        iframeUrl: String,
        context: Context,
        preferredQualityLabel: String?,
    ): AllohaResult? =
        suspendCancellableCoroutine { cont ->
            val webViewUserAgent = WebSettings.getDefaultUserAgent(context)
            var webView: WebView? = null
            var delivered = false
            var qualityProbeAttempts = 0
            var pendingQualityLabel: String? = null
            val capturedStreams = LinkedHashMap<String, CapturedStream>()
            var fallbackStream: CapturedStream? = null
            val handler = Handler(Looper.getMainLooper())
            var settleRunnable: Runnable? = null
            lateinit var timeoutRunnable: Runnable

            fun deliver(result: AllohaResult?) {
                if (!delivered) {
                    delivered = true
                    val wv = webView
                    webView = null
                    settleRunnable?.let(handler::removeCallbacks)
                    handler.removeCallbacks(timeoutRunnable)
                    handler.post { wv?.destroy() }
                    if (cont.isActive) cont.resume(result)
                }
            }

            fun resultFromCapturedStreams(): AllohaResult? {
                val qualityMap = capturedStreams.toQualityMap()
                val preferredStream = preferredQualityLabel?.let(capturedStreams::get)
                val stream = preferredStream ?: qualityMap.values.lastOrNull()?.let { url ->
                    capturedStreams.values.firstOrNull { it.url == url }
                } ?: fallbackStream

                return stream?.let {
                    val resolvedQualityMap = qualityMap.takeIf { qualities -> qualities.size > 1 }
                    AllohaResult(
                        url = it.url,
                        headers = it.headers,
                        qualities = resolvedQualityMap,
                        qualityHeaders = resolvedQualityMap
                            ?.keys
                            ?.mapNotNull { label ->
                                capturedStreams[label]?.headers?.let { headers -> label to headers }
                            }
                            ?.toMap()
                            .orEmpty(),
                    )
                }
            }

            fun scheduleDelivery(delayMs: Long = STREAM_SETTLE_DELAY_MS) {
                settleRunnable?.let(handler::removeCallbacks)
                val runnable = Runnable { deliver(resultFromCapturedStreams()) }
                settleRunnable = runnable
                handler.postDelayed(runnable, delayMs)
            }

            fun captureStream(url: String, headers: Map<String, String>) {
                val stream = CapturedStream(
                    url = url,
                    headers = headers.withAllohaStreamHeaders(
                        iframeUrl = iframeUrl,
                        userAgent = webViewUserAgent,
                    ),
                )
                fallbackStream = stream

                val label = qualityLabelFromUrl(url)
                    ?: pendingQualityLabel?.let(::cleanQualityLabel)
                    ?: "auto"
                capturedStreams[label] = stream

                scheduleDelivery()
            }

            timeoutRunnable = Runnable {
                if (resultFromCapturedStreams() == null) {
                    logExtractorFailure(
                        "Alloha",
                        iframeUrl,
                        "timed out before any stream was captured"
                    )
                }
                deliver(resultFromCapturedStreams())
            }
            handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

            val bridge = object {
                @JavascriptInterface
                fun quality(label: String) {
                    handler.post {
                        pendingQualityLabel = cleanQualityLabel(label)
                    }
                }

                @JavascriptInterface
                fun done() {
                    handler.post {
                        scheduleDelivery()
                    }
                }
            }

            fun runQualityProbe(view: WebView) {
                if (delivered || qualityProbeAttempts >= 8) return
                qualityProbeAttempts += 1
                view.evaluateJavascript(qualityProbeScript(preferredQualityLabel)) { result ->
                    if (!delivered && result.contains("no-player") && qualityProbeAttempts < 8) {
                        handler.postDelayed({ runQualityProbe(view) }, 500L)
                    }
                }
            }

            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    allowFileAccess = false
                    mediaPlaybackRequiresUserGesture = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (isStreamUrl(url)) {
                            val headers = request.requestHeaders.withAllohaStreamHeaders(
                                iframeUrl = iframeUrl,
                                userAgent = webViewUserAgent,
                            )
                            val response = runBlocking {
                                runCatching { httpClient.getText(url, headers) }.getOrNull()
                            }
                            if (response == null) {
                                logExtractorFailure(
                                    extractor = "Alloha",
                                    url = url,
                                    reason = "manifest validation request failed",
                                )
                                // A validation request can fail transiently while the signed URL is
                                // still playable by Media3. Keep the captured candidate and let the
                                // player-level recovery replace it immediately if it is rejected.
                                handler.post {
                                    captureStream(url, headers)
                                }
                                return null
                            }
                            if (response.isSuccess && response.body.contains(HLS_PLAYLIST_MARKER)) {
                                val validatedHeaders = headers.withResponseCookies(response)
                                handler.post {
                                    captureStream(url, validatedHeaders)
                                }
                            } else {
                                val denial = response.headerValue(ALLOHA_DENIAL_HEADER)
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "; $ALLOHA_DENIAL_HEADER=$it" }
                                    .orEmpty()
                                logExtractorFailure(
                                    extractor = "Alloha",
                                    url = url,
                                    reason = "manifest validation returned HTTP " +
                                            "${response.statusCode}$denial",
                                )
                            }
                            return response.toWebResourceResponse()
                        }
                        return null
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        if (qualityProbeAttempts == 0) {
                            runQualityProbe(view)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        logExtractorFailure(
                            extractor = "Alloha",
                            url = request.url.toString(),
                            reason = "WebView error ${error.errorCode}: ${error.description}",
                        )
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        val url = request.url.toString()
                        if (!isStreamUrl(url)) return
                        val denial = errorResponse.responseHeaders
                            ?.entries
                            ?.firstOrNull { it.key.equals(ALLOHA_DENIAL_HEADER, ignoreCase = true) }
                            ?.value
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "; $ALLOHA_DENIAL_HEADER=$it" }
                            .orEmpty()
                        logExtractorFailure(
                            extractor = "Alloha",
                            url = url,
                            reason = "WebView received HTTP ${errorResponse.statusCode}$denial",
                        )
                    }

                    override fun onReceivedSslError(
                        view: WebView,
                        handler: SslErrorHandler,
                        error: SslError,
                    ) {
                        logExtractorFailure(
                            extractor = "Alloha",
                            url = error.url.orEmpty(),
                            reason = "WebView SSL error ${error.primaryError}",
                        )
                        super.onReceivedSslError(view, handler, error)
                    }
                }
                addJavascriptInterface(bridge, "AllohaBridge")

                // Wrap in iframe so the Alloha page sees isFramed=true and doesn't remove itself
                val html = wrapperHtml(iframeUrl)
                loadDataWithBaseURL(
                    "https://alloha.yani.tv/",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }

            cont.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                deliver(null)
            }
        }

    private fun isStreamUrl(url: String): Boolean {
        if (!url.contains(".m3u8")) return false
        // Skip ad/analytics m3u8s
        return !url.contains("ima") && !url.contains("doubleclick") && !url.contains("ads")
    }

    private fun Map<String, String>.withAllohaStreamHeaders(
        iframeUrl: String,
        userAgent: String,
    ): Map<String, String> {
        val hasReferer = keys.any { it.equals(REFERER_HEADER, ignoreCase = true) }
        val hasOrigin = keys.any { it.equals(ORIGIN_HEADER, ignoreCase = true) }
        val downloadSafeHeaders = filterKeys { key ->
            !key.equals(USER_AGENT_HEADER, ignoreCase = true) &&
                    !key.equals(ACCESS_CONTROL_REQUEST_HEADERS_HEADER, ignoreCase = true) &&
                    !key.equals(ACCESS_CONTROL_REQUEST_METHOD_HEADER, ignoreCase = true)
        }
        return buildMap {
            putAll(downloadSafeHeaders)
            if (!hasReferer) put(REFERER_HEADER, iframeUrl)
            if (!hasOrigin) put(ORIGIN_HEADER, ALLOHA_ORIGIN)
            put(USER_AGENT_HEADER, userAgent)
        }
    }

    private fun Map<String, String>.withResponseCookies(
        response: PlayerHttpResponse,
    ): Map<String, String> {
        val responseCookies = response.setCookieHeader.takeIf { it.isNotBlank() } ?: return this
        val existingCookies = entries
            .firstOrNull { it.key.equals(COOKIE_HEADER, ignoreCase = true) }
            ?.value
            .orEmpty()
        val mergedCookies = listOf(existingCookies, responseCookies)
            .filter { it.isNotBlank() }
            .joinToString("; ")
        return filterKeys { !it.equals(COOKIE_HEADER, ignoreCase = true) } +
                (COOKIE_HEADER to mergedCookies)
    }

    private fun PlayerHttpResponse.toWebResourceResponse(): WebResourceResponse {
        val contentType = headerValue(CONTENT_TYPE_HEADER).orEmpty()
        val mimeType = contentType.substringBefore(';').ifBlank { HLS_MIME_TYPE }
        val encoding = contentType
            .substringAfter("charset=", DEFAULT_RESPONSE_ENCODING)
            .substringBefore(';')
            .trim()
            .ifBlank { DEFAULT_RESPONSE_ENCODING }
        val responseHeaders = headers
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, values) -> values.joinToString(", ") }
        return WebResourceResponse(
            mimeType,
            encoding,
            statusCode,
            if (isSuccess) HTTP_OK_REASON else HTTP_ERROR_REASON,
            responseHeaders,
            ByteArrayInputStream(bodyBytes),
        )
    }

    private fun PlayerHttpResponse.headerValue(name: String): String? =
        headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()

    private fun wrapperHtml(iframeUrl: String): String {
        val escaped = iframeUrl.replace("&", "&amp;").replace("\"", "&quot;")
        return """<!DOCTYPE html><html><head>
            <meta charset="utf-8">
            <style>*{margin:0;padding:0}html,body,iframe{width:100%;height:100%;border:none;background:#000}</style>
            </head><body>
            <script>
                try { localStorage.clear(); sessionStorage.clear(); } catch (_) {}
            </script>
            <iframe src="$escaped" allow="autoplay;fullscreen" allowfullscreen></iframe>
            </body></html>"""
    }

    private fun String.withSessionNonce(): String {
        val separator = if (contains('?')) '&' else '?'
        return "$this${separator}yummy_session=${SystemClock.elapsedRealtimeNanos()}"
    }

    private fun normalizeUrl(url: String) = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http") -> url
        else -> "https://$url"
    }

    private data class CapturedStream(val url: String, val headers: Map<String, String>)

    private fun AllohaResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
            qualityHeaders = qualityHeaders,
        )

    private fun LinkedHashMap<String, CapturedStream>.toQualityMap(): LinkedHashMap<String, String> {
        val sortedEntries = entries.sortedWith { first, second ->
            val firstQuality = qualityNumber(first.key)
            val secondQuality = qualityNumber(second.key)
            when {
                firstQuality != null && secondQuality != null -> firstQuality.compareTo(
                    secondQuality
                )

                firstQuality == null && secondQuality != null -> -1
                firstQuality != null && secondQuality == null -> 1
                else -> 0
            }
        }
        return sortedEntries.associateTo(LinkedHashMap()) { it.key to it.value.url }
    }

    private fun qualityLabelFromUrl(url: String): String? =
        QUALITY_IN_URL_REGEX.find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it in KNOWN_QUALITY_LEVELS }
            ?.let { "${it}p" }

    private fun qualityNumber(label: String): Int? =
        Regex("""(\d{3,4})""").find(label)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it in KNOWN_QUALITY_LEVELS }

    private fun cleanQualityLabel(label: String): String? {
        val cleaned = label
            .replace(Regex("""<[^>]+>"""), "")
            .trim()
            .takeIf { it.isNotEmpty() && !it.startsWith("<<<") && it != "[object Object]" }
            ?: return null

        return qualityNumber(cleaned)?.let { "${it}p" } ?: cleaned
    }

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"
        const val REFERER_HEADER = "Referer"
        const val ORIGIN_HEADER = "Origin"
        const val ACCESS_CONTROL_REQUEST_HEADERS_HEADER = "Access-Control-Request-Headers"
        const val ACCESS_CONTROL_REQUEST_METHOD_HEADER = "Access-Control-Request-Method"
        const val ALLOHA_ORIGIN = "https://alloha.yani.tv"
        const val COOKIE_HEADER = "Cookie"
        const val CONTENT_TYPE_HEADER = "Content-Type"
        const val ALLOHA_DENIAL_HEADER = "X-VD"
        const val HLS_PLAYLIST_MARKER = "#EXTM3U"
        const val HLS_MIME_TYPE = "application/vnd.apple.mpegurl"
        const val DEFAULT_RESPONSE_ENCODING = "UTF-8"
        const val HTTP_OK_REASON = "OK"
        const val HTTP_ERROR_REASON = "HTTP error"
    }

    /**
     * JavaScript probe executed inside the wrapper WebView.
     *
     * It first finds the real player inside the nested Alloha iframe, then normalizes quality labels
     * from DOM controls, Allplay config fields, HLS sources, and PlayerJS-like APIs. Only known video
     * quality numbers are kept because the player UI also contains numeric values for speed,
     * subtitles, and styling controls. Each selected quality triggers a new HLS request that is
     * captured by the WebView client.
     */
    private fun qualityProbeScript(preferredQualityLabel: String?): String {
        val preferredQuality = preferredQualityLabel?.let(::qualityNumber)?.toString() ?: "null"
        return """
        (function(){
            var targetQuality = $preferredQuality;
            function findPlayer(win){
                var names = ["player", "Player", "playerjs", "pl"];
                for (var i = 0; i < names.length; i++) {
                    try {
                        var candidate = win[names[i]];
                        if (isPlayer(candidate)) return candidate;
                    } catch(e) {}
                }
                try {
                    var el = win.document && win.document.getElementById("player");
                    if (isPlayer(el)) return el;
                } catch(e) {}
                for (var key in win) {
                    try {
                        var value = win[key];
                        if (isPlayer(value)) return value;
                    } catch(e) {}
                }
                return null;
            }
            function isPlayer(value) {
                return value && (
                    typeof value.api === "function" ||
                    (value.media && typeof value.media.querySelectorAll === "function" && "quality" in value)
                );
            }
            function labelOf(item) {
                if (typeof item === "string") return item;
                if (typeof item === "number") return String(item);
                if (item && typeof item === "object") {
                    return item.title || item.label || item.name || item.quality || item.text || item.value || item.size || "";
                }
                return String(item || "");
            }
            function addLabels(target, raw) {
                if (!raw) return;
                if (typeof raw === "string") {
                    raw.split(",").forEach(function(item){ addLabels(target, item); });
                    return;
                }
                if (Array.isArray(raw)) {
                    raw.forEach(function(item){ addLabels(target, item); });
                    return;
                }
                if (typeof raw.length === "number" && typeof raw !== "function") {
                    for (var i = 0; i < raw.length; i++) addLabels(target, raw[i]);
                    return;
                }
                var label = labelOf(raw);
                if (label) target.push(label);
            }
            function uniqueQualityLabels(labels) {
                var known = {
                    240: true,
                    360: true,
                    480: true,
                    540: true,
                    720: true,
                    1080: true,
                    1440: true,
                    2160: true
                };
                var byQuality = {};
                labels.forEach(function(label){
                    label = String(label || "").replace(/<[^>]+>/g, "").trim();
                    if (!label || label.indexOf("<<<") === 0 || label === "[object Object]") return;
                    var match = label.match(/\d{3,4}/);
                    if (!match) return;
                    var quality = parseInt(match[0], 10);
                    if (known[quality]) byQuality[quality] = String(quality);
                });
                return Object.keys(byQuality)
                    .map(function(value){ return parseInt(value, 10); })
                    .filter(function(value){ return !!value; })
                    .sort(function(a, b){ return a - b; })
                    .map(function(value){ return String(value); });
            }
            function normalizeApiQualities(raw) {
                var result = [];
                addLabels(result, raw);
                return uniqueQualityLabels(result);
            }
            function normalizeDomQualities(win, player) {
                var roots = [];
                if (player && player.media) roots.push(player.media);
                if (win.document) roots.push(win.document);

                var labels = [];
                for (var r = 0; r < roots.length; r++) {
                    var nodes = [];
                    try {
                        nodes = roots[r].querySelectorAll("source[size][src]");
                    } catch(e) {}
                    for (var i = 0; i < nodes.length; i++) {
                        labels.push(nodes[i].getAttribute("size"));
                    }

                    try {
                        nodes = roots[r].querySelectorAll(
                            "[data-allplay='quality'], [name='quality'], [role='menuitemradio'], [data-quality], [quality]"
                        );
                    } catch(e) {}
                    for (var j = 0; j < nodes.length; j++) {
                        labels.push(
                            nodes[j].getAttribute("value") ||
                            nodes[j].getAttribute("data-quality") ||
                            nodes[j].getAttribute("quality") ||
                            nodes[j].getAttribute("aria-label") ||
                            nodes[j].textContent
                        );
                    }
                }

                return uniqueQualityLabels(labels);
            }
            function normalizePlayerQualities(player) {
                var labels = [];
                try { addLabels(labels, player.config && player.config.quality && player.config.quality.options); } catch(e) {}
                try { addLabels(labels, player.options && player.options.quality); } catch(e) {}
                try { addLabels(labels, player.config && player.config.hlsSource); } catch(e) {}
                try { addLabels(labels, player.config && player.config.sources); } catch(e) {}
                try { addLabels(labels, player.sources); } catch(e) {}
                try {
                    if (player.media && typeof player.media.querySelectorAll === "function") {
                        addLabels(labels, normalizeDomQualities(window, player));
                    }
                } catch(e) {}
                return uniqueQualityLabels(labels);
            }

            var frame = document.querySelector("iframe");
            var win = frame && frame.contentWindow ? frame.contentWindow : window;
            var player = findPlayer(win);
            if (!player) {
                return "no-player";
            }

            var labels = normalizeDomQualities(win, player);
            if (!labels.length) labels = normalizePlayerQualities(player);
            var rawQualities = [];
            try {
                if (player && typeof player.api === "function") rawQualities = player.api("qualities");
            } catch(e) {}
            if (!labels.length) labels = normalizeApiQualities(rawQualities);

            var playable = [];
            for (var i = 0; i < labels.length; i++) {
                if (String(labels[i] || "").indexOf("<<<") !== 0) playable.push({ index: i, label: labels[i] });
            }
            if (targetQuality) {
                playable = playable.filter(function(item) {
                    return parseInt(item.label, 10) === targetQuality;
                });
                if (!playable.length) {
                    playable.push({ index: 0, label: String(targetQuality) });
                }
            }

            function playCurrent() {
                try {
                    player.api("play");
                } catch(e) {}
                try {
                    var video = win.document && win.document.querySelector("video");
                    if (video) video.play().catch(function(){});
                } catch(e) {}
            }
            function switchByVideoSource(quality) {
                try {
                    var source = win.document && win.document.querySelector("source[size='" + quality + "'][src]");
                    var video = win.document && win.document.querySelector("video");
                    if (!source || !video) return false;
                    video.src = source.getAttribute("src");
                    video.load();
                    video.play().catch(function(){});
                    return true;
                } catch(e) {
                    return false;
                }
            }

            if (!playable.length) {
                playCurrent();
                AllohaBridge.done();
                return JSON.stringify(labels);
            }

            var step = 0;
            function switchNextQuality() {
                if (step >= playable.length) {
                    AllohaBridge.done();
                    return;
                }
                var item = playable[step++];
                AllohaBridge.quality(String(item.label || ""));
                try {
                    var quality = parseInt(item.label, 10);
                    if (quality && typeof player.quality !== "undefined") {
                        player.quality = quality;
                    } else if (typeof player.api === "function") {
                        try {
                            player.api("quality", quality || item.label);
                        } catch(e) {
                            player.api("quality", item.index);
                        }
                    } else if (quality) {
                        switchByVideoSource(quality);
                    }
                    try {
                        frame.contentWindow.postMessage(JSON.stringify({api:"quality", value:quality}), "*");
                        frame.contentWindow.postMessage(JSON.stringify({api:"quality", value:item.index}), "*");
                    } catch(e) {}
                } catch(e) {}
                playCurrent();
                setTimeout(switchNextQuality, $QUALITY_SWITCH_DELAY_MS);
            }
            switchNextQuality();
            return JSON.stringify(labels);
        })();
    """.trimIndent()
    }
}

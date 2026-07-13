package su.afk.yummy.tv.data.player.extractor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.random.Random

/** Extracts Alloha's signed HLS session by observing the iframe's own network stack. */
internal class AllohaExtractor @Inject constructor() : PlayerStreamExtractor {
    private val extractorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun supports(url: String): Boolean = url.isAllohaPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult = withContext(Dispatchers.Main) {
        when (
            val result = openSessionViaWebView(
                iframeUrl = request.iframeUrl,
                preferredQualityLabel = request.autoQualityLabel,
                fallbackTtlSeconds = request.sessionFallbackTtlSeconds,
                context = context,
            )
        ) {
            is AllohaOpenResult.Unavailable -> PlayerStreamResolveResult.Unavailable(result.message)
            AllohaOpenResult.Failed -> PlayerStreamResolveResult.Failed
            is AllohaOpenResult.Ready -> {
                val session = result.session
                try {
                    (session as? LiveAllohaStreamSession)?.directStream ?: session.initialStream
                } finally {
                    session.close()
                }
            }
        }
    }

    suspend fun openSession(
        request: PlayerStreamRequest,
        context: Context,
    ): AllohaStreamSession? = withContext(Dispatchers.Main) {
        (
                openSessionViaWebView(
                    iframeUrl = request.iframeUrl,
                    preferredQualityLabel = request.autoQualityLabel,
                    fallbackTtlSeconds = request.sessionFallbackTtlSeconds,
                    context = context,
                ) as? AllohaOpenResult.Ready
                )?.session
    }

    private sealed interface AllohaOpenResult {
        data class Ready(val session: AllohaStreamSession) : AllohaOpenResult
        data class Unavailable(val message: String?) : AllohaOpenResult
        data object Failed : AllohaOpenResult
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private suspend fun openSessionViaWebView(
        iframeUrl: String,
        preferredQualityLabel: String?,
        fallbackTtlSeconds: Int?,
        context: Context,
    ): AllohaOpenResult = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        var delivered = false
        var streamReady = false
        var refreshedMasterReady = false
        var pendingHostChangeMaster: String? = null
        val liveSession = LiveAllohaStreamSession(handler, iframeUrl)
        val hostChangeFallback = Runnable {
            if (pendingHostChangeMaster != null) {
                // No fresh config_update confirmed the new host in time - the held headers are
                // still signed for the OLD host, so applying them would likely just 403. Force a
                // full session restart instead, same as the reference implementation does here.
                pendingHostChangeMaster = null
                Log.w(LOG_TAG, "host-change config_update timed out, forcing session restart")
                liveSession.refresh()
            }
        }
        lateinit var timeout: Runnable
        val masterWaitTimeout = Runnable {
            if (!delivered && streamReady) {
                Log.w(LOG_TAG, "refreshed master timeout, using captured quality playlist")
                liveSession.startProxy()
                delivered = true
                handler.removeCallbacks(timeout)
                if (continuation.isActive) continuation.resume(AllohaOpenResult.Ready(liveSession))
            }
        }

        fun deliverWhenReady() {
            if (delivered || !streamReady || !refreshedMasterReady) return
            liveSession.startProxy()
            delivered = true
            handler.removeCallbacks(timeout)
            handler.removeCallbacks(masterWaitTimeout)
            if (continuation.isActive) continuation.resume(AllohaOpenResult.Ready(liveSession))
        }

        fun fail(reason: AllohaOpenResult = AllohaOpenResult.Failed) {
            if (delivered) return
            delivered = true
            handler.removeCallbacksAndMessages(null)
            liveSession.close()
            if (continuation.isActive) continuation.resume(reason)
        }

        timeout = Runnable {
            logExtractorFailure("Alloha", iframeUrl, "timed out waiting for signed HLS session")
            fail()
        }
        handler.postDelayed(timeout, TIMEOUT_MS)

        val bridge = object {
            @JavascriptInterface
            fun onReady(responseJson: String, headersJson: String) {
                extractorScope.launch {
                    val parsed = runCatching {
                        Pair(
                            parseResult(responseJson, headersJson, preferredQualityLabel),
                            parseHeaders(headersJson),
                        )
                    }
                    handler.post {
                        parsed.onSuccess { (stream, headers) ->
                            CookieManager.getInstance().flush()
                            liveSession.initialize(stream)
                            liveSession.updateHeaders(headers)
                            fallbackTtlSeconds?.let(liveSession::ensureFallbackExpiry)
                            streamReady = true
                            Log.i(LOG_TAG, "ready headers=${liveSession.safeHeaderState()}")
                            deliverWhenReady()
                            if (!delivered) {
                                handler.removeCallbacks(masterWaitTimeout)
                                handler.postDelayed(masterWaitTimeout, MASTER_WAIT_TIMEOUT_MS)
                            }
                        }.onFailure {
                            logExtractorFailure(
                                "Alloha",
                                iframeUrl,
                                it.message ?: "invalid response"
                            )
                            if (it is AllohaSourceUnavailableException) {
                                // it.message is an internal debug reason (already logged above),
                                // not user-facing text - the presentation layer supplies that.
                                fail(AllohaOpenResult.Unavailable(message = null))
                            } else {
                                fail()
                            }
                        }
                    }
                }
            }

            @JavascriptInterface
            fun onConfigUpdate(edgeHash: String, ttlSeconds: Int, headersJson: String) {
                handler.post {
                    liveSession.updateHeaders(parseHeaders(headersJson) + ("accepts-controls" to edgeHash))
                    liveSession.updateExpiry(ttlSeconds)
                    val pendingMaster = pendingHostChangeMaster
                    if (pendingMaster != null) {
                        // The new edge_hash we just applied above is now current for the new
                        // host - safe to commit the master URL that was waiting on it.
                        handler.removeCallbacks(hostChangeFallback)
                        liveSession.updateMasterUrl(pendingMaster)
                        pendingHostChangeMaster = null
                        Log.i(LOG_TAG, "host change confirmed by fresh config_update")
                    }
                    Log.i(
                        LOG_TAG,
                        "config ttl=$ttlSeconds headers=${liveSession.safeHeaderState()}"
                    )
                }
            }

            @JavascriptInterface
            fun onM3u8Refreshed(url: String, headersJson: String) {
                extractorScope.launch {
                    val headers = parseHeaders(headersJson)
                    val masterUrl = url.normalizeStreamUrl()
                    handler.post {
                        val previousHost = liveSession.currentMasterUrl().hostOrNull()
                        val newHost = masterUrl.hostOrNull()
                        // Headers merge in regardless (they may carry a still-useful token); only
                        // the master URL itself is held back below when the host changed.
                        liveSession.updateHeaders(headers)
                        if (refreshedMasterReady && previousHost != null && newHost != null &&
                            previousHost != newHost
                        ) {
                            // CDN node switched mid-session: the accepts-controls token we're
                            // still holding is signed for the OLD host and would 403 on the new
                            // one. Hold the master URL update until a fresh config_update confirms
                            // the new token, falling back to a full restart if none arrives.
                            pendingHostChangeMaster = masterUrl
                            handler.removeCallbacks(hostChangeFallback)
                            handler.postDelayed(hostChangeFallback, HOST_CHANGE_CONFIG_WAIT_MS)
                            Log.w(
                                LOG_TAG,
                                "master host changed $previousHost -> $newHost, awaiting fresh config_update",
                            )
                        } else {
                            liveSession.updateMasterUrl(masterUrl)
                            refreshedMasterReady = true
                            Log.i(
                                LOG_TAG,
                                "master refreshed headers=${liveSession.safeHeaderState()}"
                            )
                            deliverWhenReady()
                        }
                    }
                }
            }

            @JavascriptInterface
            fun onStreamHeaders(headersJson: String) {
                handler.post {
                    CookieManager.getInstance().flush()
                    liveSession.updateHeaders(parseHeaders(headersJson))
                }
            }

            @JavascriptInterface
            fun onDubbingUnavailable() {
                handler.post {
                    logExtractorFailure(
                        "Alloha",
                        iframeUrl,
                        "site rendered a dubbing-unavailable message",
                    )
                    fail(AllohaOpenResult.Unavailable(message = null))
                }
            }

            @JavascriptInterface
            fun onLog(message: String) {
                Log.d(LOG_TAG, "WebView session: $message")
            }
        }

        val userAgent = desktopUserAgent()
        val parsedUrl = URL(iframeUrl)
        val baseUrl = "${parsedUrl.protocol}://${parsedUrl.host.lowercase(Locale.ROOT)}/"
        val html = wrapperHtml(iframeUrl)
        val webView = WebView(context).apply webView@{
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = userAgent
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@webView, true)
            }
            webViewClient = WebViewClient()
            addJavascriptInterface(bridge, BRIDGE_NAME)

            // The WebView is never attached to a window (extraction is headless, and for downloads
            // it runs in a background worker), so without this the browser throttles its JS timers
            // and pauses media playback - the iframe player then never fetches its correctly-signed
            // master.m3u8 (onM3u8Refreshed) and we fall back to the bnsi URL that 403s. onResume +
            // resumeTimers keep the offscreen player running, same as the reference implementation.
            onResume()
            resumeTimers()
            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
        liveSession.attach(webView) {
            // A WebView reload rotates signed session data, but the browser fingerprint must stay
            // stable for the lifetime of this Alloha session.
            webView.settings.userAgentString = userAgent
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }

        continuation.invokeOnCancellation { handler.post { liveSession.close() } }
    }

    private fun parseHeaders(headersJson: String): Map<String, String> {
        val objectValue = JSONObject(headersJson)
        return buildMap {
            objectValue.keys().forEach { key ->
                objectValue.optString(key).takeIf(String::isNotBlank)
                    ?.let { put(key.lowercase(), it) }
            }
        }
    }

    private fun parseResult(
        responseJson: String,
        headersJson: String,
        preferredQualityLabel: String?,
    ): PlayerStreamResolveResult.Stream {
        val headersObject = JSONObject(headersJson)
        val headers = buildMap {
            headersObject.keys().forEach { key -> put(key, headersObject.optString(key)) }
        }.filterValues(String::isNotBlank)

        val sources = JSONObject(responseJson).optJSONArray("hlsSource")
            ?: throw AllohaSourceUnavailableException("hlsSource is missing")
        Log.i(
            LOG_TAG,
            "bnsi hlsSources=${sources.length()} qualities=" +
                    (0 until sources.length()).map { index ->
                        sources.optJSONObject(index)?.optJSONObject("quality")?.keys()
                            ?.asSequence()?.toList().orEmpty()
                    },
        )
        val qualities = linkedMapOf<String, String>()
        for (index in 0 until sources.length()) {
            val quality = sources.optJSONObject(index)?.optJSONObject("quality") ?: continue
            quality.keys().forEach { label ->
                quality.optString(label)
                    .split(" or ")
                    .firstOrNull()
                    ?.trim()
                    ?.normalizeStreamUrl()
                    ?.takeIf(String::isNotBlank)
                    ?.let { qualities.putIfAbsent(label.normalizeQualityLabel(), it) }
            }
        }
        if (qualities.isEmpty()) throw AllohaSourceUnavailableException("no HLS qualities found")
        val sorted = qualities.entries
            .sortedBy { it.key.filter(Char::isDigit).toIntOrNull() ?: 0 }
            .associateTo(linkedMapOf()) { it.toPair() }
        val headersWithCookie = headers.toMutableMap().apply {
            sorted.values.firstNotNullOfOrNull { CookieManager.getInstance().getCookie(it) }
                ?.takeIf(String::isNotBlank)
                ?.let { put("Cookie", it) }
        }
        val preferredUrl = preferredQualityLabel
            ?.normalizeQualityLabel()
            ?.let(sorted::get)
        return PlayerStreamResolveResult.Stream(
            url = preferredUrl ?: sorted.values.last(),
            headers = headersWithCookie,
            qualities = sorted,
            qualityHeaders = sorted.keys.associateWith { headersWithCookie },
        )
    }

    private fun wrapperHtml(iframeUrl: String): String = """
        <html><body style="margin:0;background:black">
        <iframe id="alloha" src="${iframeUrl.escapeHtml()}" width="100%" height="100%" frameborder="0" allowfullscreen></iframe>
        <script>
        try {
          Object.defineProperty(document, 'visibilityState', {get:function(){return 'visible'}});
          Object.defineProperty(document, 'hidden', {get:function(){return false}});
        } catch(e) {}
        document.getElementById('alloha').onload = function() {
          try {
            var w = this.contentWindow, bnsi = null, headers = {}, done = false;
            try {
              Object.defineProperty(w.document, 'visibilityState', {get:function(){return 'visible'}});
              Object.defineProperty(w.document, 'hidden', {get:function(){return false}});
            } catch(e) {}
            var pushTimer = null;
            function put(k,v) {
              if(!k || !v) return;
              headers[String(k).toLowerCase()] = String(v);
              if(done) {
                if(pushTimer) clearTimeout(pushTimer);
                pushTimer = setTimeout(function(){ AndroidBridge.onStreamHeaders(JSON.stringify(headers)); }, 40);
              }
            }
            function ready() {
              if(done || !bnsi || !headers['authorizations'] || !headers['accepts-controls']) return;
              done = true;
              AndroidBridge.onReady(bnsi, JSON.stringify(headers));
            }
            put('origin', w.location.origin); put('referer', w.location.origin + '/');
            put('user-agent', w.navigator.userAgent); put('accept', '*/*');
            put('sec-fetch-dest', 'empty'); put('sec-fetch-mode', 'cors'); put('sec-fetch-site', 'cross-site');

            var lastMasterUrl = null;
            var primaryHost = null, fallbackHost = null, fallbackMasterUrl = null;
            function extractCdnHosts() {
              if(primaryHost || !bnsi) return;
              try {
                var data = JSON.parse(bnsi), sources = data.hlsSource;
                if(!sources || !sources[0] || !sources[0].quality) return;
                var quality = sources[0].quality, key = Object.keys(quality)[0];
                var urls = quality[key].split(' or ');
                if(urls.length < 2) return;
                var primary = urls[0].match(/https?:\/\/([^\/]+)/);
                var fallback = urls[1].trim().match(/https?:\/\/([^\/]+)/);
                if(primary) primaryHost = primary[1];
                if(fallback) {
                  fallbackHost = fallback[1];
                  fallbackMasterUrl = urls[1].trim();
                }
                AndroidBridge.onLog('CDN candidates captured');
              } catch(e) { AndroidBridge.onLog('CDN candidate parse failed'); }
            }

            var open = w.XMLHttpRequest.prototype.open;
            w.XMLHttpRequest.prototype.open = function(method,url) {
              this.__allohaUrl = url;
              this.addEventListener('load', function() {
                var url = this.responseURL || this.__allohaUrl || '';
                if(url.indexOf('/bnsi/') !== -1) {
                  bnsi = this.responseText;
                  extractCdnHosts();
                  ready();
                }
                if(done && url.indexOf('master.m3u8') !== -1 && url !== lastMasterUrl) {
                  lastMasterUrl = url;
                  AndroidBridge.onM3u8Refreshed(url, JSON.stringify(headers));
                }
              });
              return open.apply(this, arguments);
            };
            var setHeader = w.XMLHttpRequest.prototype.setRequestHeader;
            w.XMLHttpRequest.prototype.setRequestHeader = function(k,v) {
              put(k,v); ready(); return setHeader.apply(this, arguments);
            };
            var fetch = w.fetch;
            w.fetch = function(input,init) {
              try {
                var url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                if(init && init.headers) {
                  if(typeof init.headers.forEach === 'function') init.headers.forEach(function(v,k){put(k,v)});
                  else for(var k in init.headers) put(k,init.headers[k]);
                }
                ready();
                extractCdnHosts();
                if(url && (url.indexOf('.m3u8') !== -1 || url.indexOf('.ts') !== -1 || url.indexOf('.m4s') !== -1) &&
                    primaryHost && fallbackHost && url.indexOf(primaryHost) !== -1) {
                  var fallbackUrl = url.indexOf('master.m3u8') !== -1 && fallbackMasterUrl
                    ? fallbackMasterUrl : url.replace(primaryHost, fallbackHost);
                  return fetch.apply(this, arguments).then(function(response) {
                    if(response.status !== 403 && response.status !== 500 && response.status !== 503) return response;
                    AndroidBridge.onLog('Browser CDN fallback after status=' + response.status);
                    return fetch.call(w, fallbackUrl, init).then(function(fallbackResponse) {
                      if(fallbackResponse.ok && fallbackUrl.indexOf('master.m3u8') !== -1 && fallbackUrl !== lastMasterUrl) {
                        lastMasterUrl = fallbackUrl;
                        AndroidBridge.onM3u8Refreshed(fallbackUrl, JSON.stringify(headers));
                      }
                      return fallbackResponse;
                    });
                  });
                }
              } catch(e) {}
              return fetch.apply(this, arguments);
            };

            var OrigWS = w.WebSocket, send = OrigWS.prototype.send;
            var heartbeat = null, started = Date.now(), activeSocket = null, lastEdgeHash = null;
            function startHeartbeat(socket) {
              if(heartbeat) clearInterval(heartbeat);
              started = Date.now();
              heartbeat = setInterval(function() {
                if(!done || !socket || socket.readyState !== 1) return;
                try {
                  send.call(socket, JSON.stringify({type:'playing',current_time:Math.floor((Date.now()-started)/1000),resolution:'1080',track_id:'1',speed:1,subtitle:0,ts:Date.now()}));
                  AndroidBridge.onLog('heartbeat sent');
                } catch(e) { AndroidBridge.onLog('heartbeat failed'); }
              }, 25000);
            }
            function hookSocket(socket) {
              if(!socket || socket.__allohaHooked) return socket;
              socket.__allohaHooked = true;
              activeSocket = socket;
              started = Date.now();
              AndroidBridge.onLog('WebSocket hooked');
              socket.addEventListener('message', function(event) {
                try {
                  var message = JSON.parse(event.data);
                  if(message && message.type === 'config_update' && message.edge_hash && message.edge_hash !== lastEdgeHash) {
                    lastEdgeHash = message.edge_hash;
                    put('accepts-controls', message.edge_hash); ready();
                    var ttl = message.ttl || 120;
                    AndroidBridge.onLog('config_update ttl=' + ttl);
                    AndroidBridge.onConfigUpdate(message.edge_hash, ttl, JSON.stringify(headers));
                  }
                } catch(e) {}
              });
              socket.addEventListener('open', function() {
                AndroidBridge.onLog('WebSocket opened');
                startHeartbeat(socket);
              });
              socket.addEventListener('close', function(event){
                if(activeSocket === socket) {
                  activeSocket = null;
                  if(heartbeat) clearInterval(heartbeat);
                }
                var reason = event && event.reason ? String(event.reason).replace(/\s+/g, ' ').slice(0, 80) : '';
                AndroidBridge.onLog('WebSocket closed code=' + (event ? event.code : 0) +
                  ' clean=' + (event ? event.wasClean : false) + ' reason=' + reason);
              });
              if(socket.readyState === 1) startHeartbeat(socket);
              return socket;
            }
            OrigWS.prototype.send = function(data) {
              hookSocket(this);
              return send.call(this,data);
            };
            w.WebSocket = function(url, protocols) {
              return hookSocket(protocols ? new OrigWS(url, protocols) : new OrigWS(url));
            };
            w.WebSocket.prototype = OrigWS.prototype;
            w.WebSocket.CONNECTING = OrigWS.CONNECTING;
            w.WebSocket.OPEN = OrigWS.OPEN;
            w.WebSocket.CLOSING = OrigWS.CLOSING;
            w.WebSocket.CLOSED = OrigWS.CLOSED;
            var errorReported = false;
            var unavailablePattern = /озвучка\s*недоступна/i;
            setInterval(function() {
              if(!done && !errorReported) {
                try {
                  var text = w.document.body ? w.document.body.textContent : '';
                  if(text && unavailablePattern.test(text)) {
                    errorReported = true;
                    AndroidBridge.onDubbingUnavailable();
                    return;
                  }
                } catch(e) {}
              }
              // Keep the iframe player actively playing even AFTER the session is captured. If we
              // stop at 'done', the player pauses and tears down its WebSocket (~2.4s) before it
              // ever fetches its correctly-signed master.m3u8 (onM3u8Refreshed), forcing us onto the
              // raw bnsi URL that the CDN rejects with 403 token_decrypt. Keeping it playing makes
              // the player keep (re)fetching its own master and keeps the socket alive for heartbeats.
              var button = w.document.querySelector('.allplay__play-btn'); if(button) button.click();
              var video = w.document.querySelector('video');
              if(video) { video.muted = true; if(video.paused) video.play().catch(function(){}); }
            }, 1500);
          } catch(e) { AndroidBridge.onLog(String(e)); }
        };
        </script></body></html>
    """.trimIndent()

    private fun String.normalizeStreamUrl(): String = if (startsWith("//")) "https:$this" else this
    private fun String.hostOrNull(): String? =
        runCatching { URL(this).host }.getOrNull()?.takeIf(String::isNotBlank)

    private fun String.normalizeQualityLabel(): String =
        trim().let { if (it.endsWith("p")) it else "${it}p" }

    private fun String.escapeHtml(): String = replace("&", "&amp;").replace("\"", "&quot;")
    private fun String.asDesktopUserAgent(): String = replace(Regex(";?\\s*(wv|Mobile)\\b"), "")

    private fun desktopUserAgent(): String {
        val os = DESKTOP_OS.random()
        val version = Random.nextInt(130, 136)
        return "Mozilla/5.0 ($os) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$version.0.0.0 Safari/537.36"
    }

    private companion object {
        const val LOG_TAG = "AllohaExtractor"
        val DESKTOP_OS = listOf(
            "Windows NT 10.0; Win64; x64",
            "Windows NT 11.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "Macintosh; Intel Mac OS X 14_4_1",
            "X11; Linux x86_64",
            "X11; Ubuntu; Linux x86_64",
        )
        const val BRIDGE_NAME = "AndroidBridge"
        const val TIMEOUT_MS = 30_000L

        // How long to wait for the iframe's own master.m3u8 (onM3u8Refreshed) before falling back
        // to the raw bnsi quality URL. The bnsi URL carries a path token the CDN rejects with 403
        // token_decrypt once the live session is established; only the master the iframe itself
        // fetches is signed correctly, so the fallback must stay a genuine last resort. The wrapper
        // JS keeps the iframe player actively playing (even after the session is captured) so it
        // reliably (re)fetches that master within a couple of seconds; this window just needs a
        // little headroom over that.
        const val MASTER_WAIT_TIMEOUT_MS = 6_000L
        const val HOST_CHANGE_CONFIG_WAIT_MS = 10_000L

        fun safeFingerprint(value: String?): String {
            if (value.isNullOrBlank()) return "none"
            return MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .take(4)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }

    private class LiveAllohaStreamSession(
        private val handler: Handler,
        override val sourceKey: String,
    ) : AllohaStreamSession {
        override val id: String = UUID.randomUUID().toString()
        private val headers = ConcurrentHashMap<String, String>()
        private val masterUrl = AtomicReference("")
        private val generation = AtomicLong(0L)
        private val expiry = AtomicLong(0L)
        private val view = AtomicReference<WebView?>(null)
        private val stream = AtomicReference<PlayerStreamResolveResult.Stream?>(null)
        private val refreshAction = AtomicReference<(() -> Unit)?>(null)
        private val qualityMasters = ConcurrentHashMap<String, String>()
        private val selectedQuality = AtomicReference<String?>(null)
        private val proxy = AtomicReference<AllohaStreamProxy?>(null)
        private val streamStateLock = Any()

        override val initialStream: PlayerStreamResolveResult.Stream
            get() {
                val value = checkNotNull(stream.get())
                val selectedPlaybackUrl = selectedQuality.get()
                    ?.let { checkNotNull(proxy.get()).qualityUrl(it) }
                    ?: playbackUrl
                return value.copy(
                    url = selectedPlaybackUrl,
                    headers = emptyMap(),
                    qualities = qualityUrls,
                    qualityHeaders = emptyMap(),
                )
            }
        val directStream: PlayerStreamResolveResult.Stream
            get() {
                val value = checkNotNull(stream.get())
                val currentHeaders = currentHeaders()
                return value.copy(
                    headers = currentHeaders,
                    qualityHeaders = value.qualities.orEmpty().keys.associateWith { currentHeaders },
                )
            }
        override val playbackUrl: String
            get() = checkNotNull(proxy.get()).playbackUrl
        override val qualityUrls: LinkedHashMap<String, String>
            get() = qualityMasters.keys
                .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
                .associateTo(linkedMapOf()) { it to checkNotNull(proxy.get()).qualityUrl(it) }

        fun attach(webView: WebView, refresh: () -> Unit) {
            view.set(webView)
            refreshAction.set(refresh)
        }

        fun initialize(value: PlayerStreamResolveResult.Stream) {
            synchronized(streamStateLock) {
                stream.set(value)
                masterUrl.set(value.url)
                qualityMasters.clear()
                value.qualities?.let(qualityMasters::putAll)
                selectedQuality.set(
                    value.qualities?.entries?.firstOrNull { it.value == value.url }?.key
                )
                headers.clear()
                headers.putAll(value.headers.mapKeys { it.key.lowercase() })
                generation.incrementAndGet()
            }
        }

        fun startProxy() {
            if (proxy.get() != null) return
            Log.i(LOG_TAG, "starting localhost proxy ${safeHeaderState()}")
            proxy.compareAndSet(
                null,
                AllohaStreamProxy(
                    streamStateProvider = ::currentStreamState,
                    qualityMasterProvider = qualityMasters::get,
                    requestSessionRefresh = ::refresh,
                )
            )
        }

        fun updateHeaders(value: Map<String, String>) {
            synchronized(streamStateLock) {
                headers.putAll(value.mapKeys { it.key.lowercase() })
            }
        }

        fun updateMasterUrl(value: String) {
            synchronized(streamStateLock) {
                if (value.isNotBlank()) masterUrl.set(value)
            }
        }

        fun updateExpiry(ttlSeconds: Int) {
            expiry.set(System.currentTimeMillis() + ttlSeconds * 1_000L)
        }

        fun ensureFallbackExpiry(ttlSeconds: Int) {
            if (expiry.get() <= System.currentTimeMillis()) updateExpiry(ttlSeconds)
        }

        override fun currentHeaders(): Map<String, String> = synchronized(streamStateLock) {
            headers.toMap()
        }

        private fun currentStreamState(): AllohaStreamState = synchronized(streamStateLock) {
            AllohaStreamState(
                headers = headers.toMap(),
                masterUrl = masterUrl.get(),
                generation = generation.get(),
                expiresAtMs = expiry.get().takeIf { it > 0L },
            )
        }

        fun safeHeaderState(): String = synchronized(streamStateLock) {
            val host = runCatching { URL(masterUrl.get()).host }.getOrDefault("unknown")
            val ttlSeconds = expiry.get().takeIf { it > 0L }
                ?.let { ((it - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L) }
            val cookie = sequenceOf(masterUrl.get(), headers["origin"], headers["referer"])
                .filterNotNull()
                .filter(String::isNotBlank)
                .mapNotNull {
                    runCatching { CookieManager.getInstance().getCookie(it) }.getOrNull()
                }
                .firstOrNull(String::isNotBlank)
            "generation=${generation.get()} host=$host ttl=${ttlSeconds ?: "none"} " +
                    "names=${headers.keys.sorted()} " +
                    "auth=${safeFingerprint(headers["authorizations"])} " +
                    "controls=${safeFingerprint(headers["accepts-controls"])} " +
                    "ua=${safeFingerprint(headers["user-agent"])} " +
                    "cookie=${safeFingerprint(cookie)}"
        }

        override fun currentMasterUrl(): String = synchronized(streamStateLock) { masterUrl.get() }
        override fun expiresAtMs(): Long? = expiry.get().takeIf { it > 0L }
        override fun refresh() {
            expiry.set(0L)
            handler.post {
                refreshAction.get()?.invoke()
            }
        }

        override fun selectQuality(label: String) {
            if (qualityMasters.containsKey(label)) selectedQuality.set(label)
        }

        override fun close() {
            handler.post {
                proxy.getAndSet(null)?.close()
                refreshAction.set(null)
                view.getAndSet(null)?.let {
                    it.removeJavascriptInterface(BRIDGE_NAME)
                    it.stopLoading()
                    it.destroy()
                }
            }
        }
    }
}

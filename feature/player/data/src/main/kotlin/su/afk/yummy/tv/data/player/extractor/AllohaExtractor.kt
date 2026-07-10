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
        val session =
            openSession(request, context) ?: return@withContext PlayerStreamResolveResult.Failed
        try {
            (session as? LiveAllohaStreamSession)?.directStream ?: session.initialStream
        } finally {
            session.close()
        }
    }

    suspend fun openSession(
        request: PlayerStreamRequest,
        context: Context,
    ): AllohaStreamSession? = withContext(Dispatchers.Main) {
        openSessionViaWebView(
            iframeUrl = request.iframeUrl,
            preferredQualityLabel = request.autoQualityLabel,
            fallbackTtlSeconds = request.sessionFallbackTtlSeconds,
            context = context,
        )
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private suspend fun openSessionViaWebView(
        iframeUrl: String,
        preferredQualityLabel: String?,
        fallbackTtlSeconds: Int?,
        context: Context,
    ): AllohaStreamSession? = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        var delivered = false
        var streamReady = false
        var refreshedMasterReady = false
        val liveSession = LiveAllohaStreamSession(handler, iframeUrl)
        lateinit var timeout: Runnable
        val masterWaitTimeout = Runnable {
            if (!delivered && streamReady) {
                Log.w(LOG_TAG, "refreshed master timeout, using captured quality playlist")
                liveSession.startProxy()
                delivered = true
                handler.removeCallbacks(timeout)
                if (continuation.isActive) continuation.resume(liveSession)
            }
        }

        fun deliverWhenReady() {
            if (delivered || !streamReady || !refreshedMasterReady) return
            liveSession.startProxy()
            delivered = true
            handler.removeCallbacks(timeout)
            handler.removeCallbacks(masterWaitTimeout)
            if (continuation.isActive) continuation.resume(liveSession)
        }

        fun fail() {
            if (delivered) return
            delivered = true
            handler.removeCallbacksAndMessages(null)
            liveSession.close()
            if (continuation.isActive) continuation.resume(null)
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
                        Triple(
                            parseResult(responseJson, headersJson, preferredQualityLabel),
                            parseFallbackMaster(responseJson, preferredQualityLabel),
                            parseHeaders(headersJson),
                        )
                    }
                    handler.post {
                        parsed.onSuccess { (stream, fallbackMaster, headers) ->
                            liveSession.initialize(stream)
                            liveSession.updateFallbackMaster(fallbackMaster)
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
                            fail()
                        }
                    }
                }
            }

            @JavascriptInterface
            fun onConfigUpdate(edgeHash: String, ttlSeconds: Int, headersJson: String) {
                handler.post {
                    liveSession.updateHeaders(parseHeaders(headersJson) + ("accepts-controls" to edgeHash))
                    liveSession.updateExpiry(ttlSeconds)
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
                        liveSession.updateStreamState(headers, masterUrl)
                        refreshedMasterReady = true
                        Log.i(LOG_TAG, "master refreshed headers=${liveSession.safeHeaderState()}")
                        deliverWhenReady()
                    }
                }
            }

            @JavascriptInterface
            fun onStreamHeaders(headersJson: String) {
                liveSession.updateHeaders(parseHeaders(headersJson))
            }

            @JavascriptInterface
            fun onLog(message: String) = Unit
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

            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
        liveSession.attach(webView) {
            webView.settings.userAgentString = desktopUserAgent()
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
            ?: error("hlsSource is missing")
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
        check(qualities.isNotEmpty()) { "no HLS qualities found" }
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

    private fun parseFallbackMaster(
        responseJson: String,
        preferredQualityLabel: String?,
    ): String? {
        val sources = JSONObject(responseJson).optJSONArray("hlsSource") ?: return null
        val fallbacks = linkedMapOf<String, String>()
        for (index in 0 until sources.length()) {
            val quality = sources.optJSONObject(index)?.optJSONObject("quality") ?: continue
            quality.keys().forEach { label ->
                quality.optString(label).split(" or ").getOrNull(1)?.trim()
                    ?.normalizeStreamUrl()?.takeIf(String::isNotBlank)
                    ?.let { fallbacks[label.normalizeQualityLabel()] = it }
            }
        }
        val preferred = preferredQualityLabel?.normalizeQualityLabel()
        return preferred?.let(fallbacks::get)
            ?: fallbacks.maxByOrNull { it.key.filter(Char::isDigit).toIntOrNull() ?: 0 }?.value
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

            var open = w.XMLHttpRequest.prototype.open;
            w.XMLHttpRequest.prototype.open = function(method,url) {
              this.__allohaUrl = url;
              this.addEventListener('load', function() {
                var url = this.responseURL || this.__allohaUrl || '';
                if(url.indexOf('/bnsi/') !== -1) { bnsi = this.responseText; ready(); }
                if(done && url.indexOf('master.m3u8') !== -1) AndroidBridge.onM3u8Refreshed(url, JSON.stringify(headers));
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
                if(init && init.headers) {
                  if(typeof init.headers.forEach === 'function') init.headers.forEach(function(v,k){put(k,v)});
                  else for(var k in init.headers) put(k,init.headers[k]);
                }
                ready();
              } catch(e) {}
              return fetch.apply(this, arguments);
            };

            var OrigWS = w.WebSocket, send = OrigWS.prototype.send;
            var heartbeat = null, started = Date.now();
            function startHeartbeat(socket) {
              if(heartbeat) clearInterval(heartbeat);
              started = Date.now();
              heartbeat = setInterval(function() {
                if(socket.readyState !== 1) return;
                try { send.call(socket, JSON.stringify({type:'playing',current_time:Math.floor((Date.now()-started)/1000),resolution:'1080',track_id:'1',speed:1,subtitle:0,ts:Date.now()})); } catch(e) {}
              }, 25000);
            }
            function hookSocket(socket) {
              if(!socket || socket.__allohaHooked) return socket;
              socket.__allohaHooked = true;
              socket.addEventListener('message', function(event) {
                try {
                  var message = JSON.parse(event.data);
                  if(message && message.type === 'config_update' && message.edge_hash) {
                    put('accepts-controls', message.edge_hash); ready();
                    AndroidBridge.onConfigUpdate(message.edge_hash, message.ttl || 120, JSON.stringify(headers));
                  }
                } catch(e) {}
              });
              socket.addEventListener('open', function() { startHeartbeat(socket); });
              socket.addEventListener('close', function(){if(heartbeat) clearInterval(heartbeat)});
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
            setInterval(function() {
              if(done) return;
              var button = w.document.querySelector('.allplay__play-btn'); if(button) button.click();
              var video = w.document.querySelector('video');
              if(video) { video.muted = true; if(video.paused) video.play().catch(function(){}); }
            }, 1500);
          } catch(e) { AndroidBridge.onLog(String(e)); }
        };
        </script></body></html>
    """.trimIndent()

    private fun String.normalizeStreamUrl(): String = if (startsWith("//")) "https:$this" else this
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
        const val MASTER_WAIT_TIMEOUT_MS = 2_000L
    }

    private class LiveAllohaStreamSession(
        private val handler: Handler,
        override val sourceKey: String,
    ) : AllohaStreamSession {
        override val id: String = UUID.randomUUID().toString()
        private val headers = ConcurrentHashMap<String, String>()
        private val masterUrl = AtomicReference("")
        private val fallbackMasterUrl = AtomicReference("")
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
            stream.set(value)
            masterUrl.set(value.url)
            qualityMasters.clear()
            value.qualities?.let(qualityMasters::putAll)
            selectedQuality.set(
                value.qualities?.entries?.firstOrNull { it.value == value.url }?.key
            )
            headers.putAll(value.headers.mapKeys { it.key.lowercase() })
        }

        fun startProxy() {
            if (proxy.get() != null) return
            proxy.compareAndSet(
                null,
                AllohaStreamProxy(
                    streamStateProvider = ::currentStreamState,
                    qualityMasterProvider = qualityMasters::get,
                    fallbackMasterProvider = fallbackMasterUrl::get,
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

        fun updateStreamState(value: Map<String, String>, master: String) {
            synchronized(streamStateLock) {
                headers.putAll(value.mapKeys { it.key.lowercase() })
                if (master.isNotBlank()) masterUrl.set(master)
            }
        }

        fun updateFallbackMaster(value: String?) {
            if (!value.isNullOrBlank()) fallbackMasterUrl.set(value)
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

        private fun currentStreamState(): Pair<Map<String, String>, String> =
            synchronized(streamStateLock) { headers.toMap() to masterUrl.get() }

        fun safeHeaderState(): String =
            "names=${headers.keys.sorted()} auth=${headers.containsKey("authorizations")} " +
                    "controls=${headers.containsKey("accepts-controls")}"

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

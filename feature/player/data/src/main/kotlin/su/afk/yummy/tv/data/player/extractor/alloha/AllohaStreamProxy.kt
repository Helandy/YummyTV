package su.afk.yummy.tv.data.player.extractor.alloha

import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.ioScope
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal data class AllohaStreamState(
    val headers: Map<String, String>,
    val masterUrl: String,
    val generation: Long,
    val expiresAtMs: Long?,
)

/**
 * Local loopback HLS proxy for a live Alloha session. Beyond simple request forwarding it mirrors
 * the recovery techniques of the reference implementation (alloha-parser-kotlin):
 *  1. An exact same-request retry on a fresh connection for transient 403 responses.
 *  2. A small segment cache + prefetch + in-flight de-dup to smooth out playback.
 *  3. A cheap "rewrite to the current session's path" retry for stale-path segment 403s.
 *  4. Rebuilding the OkHttp client/connection pool whenever the session generation changes, so no
 *     stale keep-alive connections survive a session refresh - while keeping the segment cache and
 *     the fetches already in flight, which the previous token already authorized.
 *  5. Serving the reference 188-byte empty TS packet for unrecoverable `-a1.ts`/`-a2.ts` audio
 *     segments, while every other failed media segment remains an explicit transport error.
 *  6. A single-flight escalation to one full session refresh after bounded local recovery.
 */
internal class AllohaStreamProxy(
    private val streamStateProvider: () -> AllohaStreamState,
    private val masterProvider: (audioId: String?, quality: String?) -> String?,
    private val requestSessionRefresh: () -> Unit,
    private val analytics: AnalyticsTracker,
) : AutoCloseable {
    private val scope = ioScope()

    // Bound to the IPv4 loopback explicitly rather than InetAddress.getLoopbackAddress(): on a
    // device that prefers IPv6 the latter resolves to ::1, so nothing ends up listening on the
    // 127.0.0.1 that every URL below points at and Media3 fails each request with a
    // ConnectException. Pinning both ends to the same literal keeps them in step, and a literal
    // address needs no name resolution.
    private val server = ServerSocket(0, 8, InetAddress.getByName(LOOPBACK_HOST))

    // Per-session secret so that no other app on the device (loopback ports aren't isolated
    // between apps of the same Android user) can drive this proxy even while it's bound to
    // loopback. UUID.randomUUID() is backed by SecureRandom.
    private val sessionToken = UUID.randomUUID().toString()

    // Verbose diagnostics are off by default: safeSummary() costs four SHA-256 digests plus up to
    // three CookieManager lookups, and logPlaylistAudioState() re-scans the whole playlist - too
    // much to pay on every segment/playlist request. Turn on at runtime when debugging with
    // `adb shell setprop log.tag.AllohaStreamProxy DEBUG`.
    private val verbose = Log.isLoggable(LOG_TAG, Log.DEBUG)

    private val lastSeenGeneration = AtomicLong(-1L)
    private val rejectedSessionGeneration = AtomicLong(-1L)
    private val lastRejectionMarker = AtomicReference<String?>(null)

    @Volatile
    private var connectionPool = buildConnectionPool()

    @Volatile
    private var client = buildClient()

    private val sessionRefreshLock = Any()
    private val closed = AtomicBoolean(false)
    private val backgroundRefreshScheduled = AtomicBoolean(false)
    private val backgroundRefreshConsumedSinceSuccess = AtomicBoolean(false)

    @Volatile
    private var sessionRefreshInProgress = false

    private val cacheLock = Any()
    private val segmentCache =
        object : LinkedHashMap<String, ByteArray>(CACHE_CAPACITY, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>): Boolean =
                size > CACHE_CAPACITY
        }
    private val inFlightSegments = ConcurrentHashMap<String, Deferred<FetchResult?>>()

    @Volatile
    private var recentSegments: List<String> = emptyList()

    val playbackUrl = "http://$LOOPBACK_HOST:${server.localPort}/master.m3u8?token=$sessionToken"

    init {
        analytics.log(LOG_TAG) { "Proxy started port=${server.localPort} ${streamStateProvider().summary()}" }
        scope.launch {
            while (isActive) {
                runCatching { server.accept() }.getOrNull()
                    ?.let { socket -> launch { runSuspendCatching { handle(socket) } } }
            }
        }
    }

    /**
     * Playback URL for a specific dubbing/quality pair. Both selections travel in the loopback URL
     * so switching either one is just a MediaItem swap against the same live session.
     */
    fun streamUrl(audioId: String?, quality: String?): String = buildString {
        append(playbackUrl)
        val params = buildList {
            audioId?.takeIf(String::isNotBlank)
                ?.let { add("audio=${URLEncoder.encode(it, "UTF-8")}") }
            quality?.takeIf(String::isNotBlank)
                ?.let { add("quality=${URLEncoder.encode(it, "UTF-8")}") }
        }
        if (params.isNotEmpty()) append("&").append(params.joinToString("&"))
    }

    /** Proxies an arbitrary session URL (e.g. a subtitle file) through this session's headers. */
    fun sideloadUrl(url: String): String {
        val encoded = Base64.encodeToString(url.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        return "http://$LOOPBACK_HOST:${server.localPort}/proxy?url=$encoded&token=$sessionToken"
    }

    private fun buildConnectionPool(): ConnectionPool = ConnectionPool(5, 20, TimeUnit.SECONDS)

    private fun buildClient(): OkHttpClient = OkHttpClient.Builder()
        .connectionPool(connectionPool)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Rebuilds transport state for every fresh WebView session, even if its master URL is reused. */
    private fun noteActiveState(state: AllohaStreamState) {
        if (state.generation <= 0L) return
        while (true) {
            val previous = lastSeenGeneration.get()
            if (state.generation <= previous) return
            if (lastSeenGeneration.compareAndSet(previous, state.generation)) {
                if (previous >= 0L) onSessionRotated(state)
                return
            }
        }
    }

    private fun onSessionRotated(state: AllohaStreamState) {
        // Only the transport is rebuilt. The cached segment bytes and the fetches already in flight
        // were authorized by the previous token and stay perfectly valid - dropping them would just
        // force Media3 to redownload content we already have, which is visible as a stall right at
        // the rotation. The new client/pool is what matters: no keep-alive connection from the old
        // session may survive into the new one.
        //
        // A bare evictAll() on the existing pool is NOT enough for that: it closes idle connections
        // only, so a connection that happens to be mid-call during the eviction is returned to that
        // same pool afterwards and can be picked up again by the new session. Swapping in a fresh
        // pool is what actually keeps the two sessions' connections apart.
        val oldPool = connectionPool
        connectionPool = buildConnectionPool()
        client = buildClient()
        // evictAll() closes the idle connections only, so nothing from the old session can be
        // picked up again, while the calls still running on it are allowed to finish.
        oldPool.evictAll()
        analytics.log(LOG_TAG) { "Session transport reset ${state.summary()}" }
    }

    private suspend fun handle(socket: Socket) {
        socket.use {
            val reader = it.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: return
            val requestHeaders = buildMap {
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) put(
                        line.substring(0, separator).trim().lowercase(),
                        line.substring(separator + 1).trim(),
                    )
                }
            }
            val path = requestLine.split(' ').getOrNull(1) ?: return
            // Parsed once: this runs per segment, and the `url=` value is a long base64 blob, so
            // rescanning the path for every parameter is real work on the hot path. A proper
            // name/value split also avoids matching a parameter name as a mere substring.
            val params = parseQuery(path)

            // Gate every request on the per-session token before doing any work with it, so an
            // unrelated caller on the same device can't drive the proxy even though it's bound
            // to loopback.
            if (params["token"] != sessionToken) {
                send(it.getOutputStream(), 404, "text/plain", byteArrayOf())
                return
            }

            val state = streamStateProvider()
            noteActiveState(state)
            val target = if (path.substringBefore('?') == "/master.m3u8") {
                masterProvider(params["audio"], params["quality"]) ?: state.masterUrl
            } else {
                params["url"]?.let { encoded ->
                    runCatching {
                        String(Base64.decode(encoded, Base64.URL_SAFE))
                    }.getOrNull()
                }.orEmpty()
            }
            // Only a syntactic check, deliberately: anything that needs the network (name
            // resolution in particular) must not sit in front of every segment. A 404 here is a
            // fatal response code for Media3 (see PlayerLoadErrorHandlingPolicy), so it may only
            // ever mean "this request is malformed", never "the network hiccuped" - the latter has
            // to reach fetchWithRecovery and come back as a retriable 503.
            if (!target.isHttpUrl()) {
                send(it.getOutputStream(), 404, "text/plain", byteArrayOf())
                return
            }
            serve(target, requestHeaders, it.getOutputStream())
        }
    }

    private suspend fun serve(
        url: String,
        requestHeaders: Map<String, String>,
        output: OutputStream
    ) {
        if (isPlaylistUrl(url)) {
            servePlaylist(url, output)
        } else {
            serveSegment(url, requestHeaders, output)
        }
    }

    private suspend fun servePlaylist(url: String, output: OutputStream) {
        val result = fetchWithRecovery(url)
        if (result == null) {
            send(output, 503, "text/plain", byteArrayOf())
            return
        }
        val text = result.bytes.toString(Charsets.UTF_8)
        if (verbose) logPlaylistAudioState(text)
        val rewritten = rewritePlaylist(text, result.finalUrl)
        send(output, 200, "application/vnd.apple.mpegurl", rewritten.toByteArray())
    }

    private suspend fun serveSegment(
        url: String,
        requestHeaders: Map<String, String>,
        output: OutputStream
    ) {
        // Range requests bypass the cache/prefetch path so byte-range semantics stay exact.
        val hasRange = requestHeaders.containsKey("range") || requestHeaders.containsKey("if-range")
        if (hasRange) {
            val result = fetchWithRecovery(url, requestHeaders, allowRange = true)
            if (result == null) {
                send(output, 503, "text/plain", byteArrayOf())
            } else {
                sendBytes(output, result)
            }
            return
        }

        val cached = synchronized(cacheLock) { segmentCache[url] }
        val result = if (cached != null) {
            FetchResult(cached, 200, contentTypeFor(url), url)
        } else {
            getOrFetch(url)
        }

        if (result == null) {
            if (url.contains("-a1.ts") || url.contains("-a2.ts")) {
                analytics.log(LOG_TAG) {
                    "Audio TS segment unrecoverable, serving empty packet: ${
                        url.takeLast(
                            80
                        )
                    }"
                }
                sendBytes(output, FetchResult(EMPTY_TS_PACKET, 200, "video/MP2T", url))
            } else {
                analytics.log(LOG_TAG) { "Media segment unrecoverable: ${url.takeLast(80)}" }
                send(output, 503, "text/plain", byteArrayOf())
            }
            return
        }
        if (result.bytes.size < MIN_SEGMENT_BYTES_HINT) {
            analytics.log(LOG_TAG) {
                "Serving suspiciously small segment (${result.bytes.size}b): ${
                    url.takeLast(
                        80
                    )
                }"
            }
        }
        sendBytes(output, result)
        prefetchUpcoming(url)
    }

    private suspend fun getOrFetch(url: String): FetchResult? {
        synchronized(cacheLock) { segmentCache[url] }?.let {
            return FetchResult(
                it,
                200,
                contentTypeFor(url),
                url
            )
        }
        // computeIfAbsent (not the generic getOrPut) so concurrent requests for the same
        // segment truly share one in-flight fetch instead of racing to start their own.
        val deferred = inFlightSegments.computeIfAbsent(url) {
            scope.async {
                fetchWithRecovery(url)?.also { result ->
                    synchronized(cacheLock) { segmentCache[url] = result.bytes }
                }
            }
        }
        return try {
            deferred.await()
        } catch (_: CancellationException) {
            // The loopback caller still needs a complete HTTP response, so retry this one segment
            // rather than letting the socket end at EOF.
            if (scope.isActive) fetchWithRecovery(url) else null
        } finally {
            inFlightSegments.remove(url, deferred)
        }
    }

    private fun prefetchUpcoming(url: String) {
        val list = recentSegments
        val idx = list.indexOf(url)
        if (idx < 0) return
        for (i in 1..PREFETCH_COUNT) {
            val next = list.getOrNull(idx + i) ?: break
            if (synchronized(cacheLock) { segmentCache.containsKey(next) }) continue
            if (inFlightSegments.containsKey(next)) continue
            scope.launch { getOrFetch(next) }
        }
    }

    private data class FetchResult(
        val bytes: ByteArray,
        val statusCode: Int,
        val contentType: String,
        val finalUrl: String,
        val contentRange: String? = null,
        val acceptRanges: String? = null,
    )

    private suspend fun fetchWithRecovery(
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        allowRange: Boolean = false,
    ): FetchResult? {
        val initialState = streamStateProvider()
        noteActiveState(initialState)

        var rejectedGeneration = -1L
        var rejectedMarker: String? = null

        fun execute(
            target: String,
            forwardRange: Boolean = allowRange,
        ): FetchResult? {
            var statusCode: Int? = null
            val state = streamStateProvider()
            noteActiveState(state)
            val currentHeaders = state.headers
            val cookie = sequenceOf(target, currentHeaders["origin"], currentHeaders["referer"])
                .filterNotNull()
                .mapNotNull {
                    runCatching { CookieManager.getInstance().getCookie(it) }.getOrNull()
                }
                .firstOrNull(String::isNotBlank)
            val request = Request.Builder().url(target).apply {
                // Forward the headers the iframe player sent, except the ones in
                // BLOCKED_FORWARD_HEADERS (transport headers + per-request nonces like borth). The
                // CDN's token decryption keys off the stable anti-bot headers accepts-controls and
                // authorizations; those must always be forwarded.
                currentHeaders.forEach { (name, value) ->
                    if (name.lowercase() !in BLOCKED_FORWARD_HEADERS) {
                        header(name, value)
                    }
                }
                if (currentHeaders.keys.none { it.equals("accept", ignoreCase = true) }) {
                    header("Accept", "*/*")
                }
                // Headers a real Chrome always sends but the page never sets explicitly, so the
                // wrapper's setRequestHeader/fetch hooks never capture them. Without these the CDN
                // sees a Chrome user-agent with no client hints and no language - an obvious
                // mismatch. Derived from the UA we present so the two agree with each other.
                if (currentHeaders.keys.none { it.equals("accept-language", ignoreCase = true) }) {
                    header("Accept-Language", DEFAULT_ACCEPT_LANGUAGE)
                }
                val presentedUa = currentHeaders["user-agent"].orEmpty()
                CHROME_VERSION.find(presentedUa)?.groupValues?.get(1)?.let { major ->
                    header(
                        "sec-ch-ua",
                        "\"Chromium\";v=\"$major\", \"Google Chrome\";v=\"$major\", " +
                            "\"Not_A Brand\";v=\"24\"",
                    )
                    header("sec-ch-ua-mobile", "?0")
                    header("sec-ch-ua-platform", platformHintFor(presentedUa))
                }
                cookie?.let { header("Cookie", it) }
                if (forwardRange) {
                    requestHeaders["range"]?.let { header("Range", it) }
                    requestHeaders["if-range"]?.let { header("If-Range", it) }
                }
            }.build()

            // The exact header set we present to the CDN. A mismatch here (a Chrome UA with no
            // Accept-Language and no client hints) is what earned 403 client_blocked, so this is
            // the first thing to check when the CDN starts refusing the proxy while the page's
            // own player still loads fine.
            if (verbose) analytics.log(LOG_TAG) {
                "outgoing " + request.headers.names().joinToString(",") +
                    " | ua=" + (request.header("User-Agent") ?: "-").take(48) +
                    " | accept=" + (request.header("Accept") ?: "-") +
                    " | lang=" + (request.header("Accept-Language") ?: "MISSING") +
                    " | chUa=" + (request.header("sec-ch-ua") ?: "MISSING") +
                    " | origin=" + (request.header("Origin") ?: "-") +
                    " | referer=" + (request.header("Referer") ?: "-") +
                    " | cookie=" + (if (request.header("Cookie") != null) "yes" else "no")
            }
            val requestClient = client
            val requestPool = connectionPool
            fun perform(requestToSend: Request, connectionCloseRetry: Boolean): FetchResult? =
                runCatching {
                    requestClient.newCall(requestToSend).execute().use { response ->
                        statusCode = response.code
                        if (response.code !in SUCCESS_CODES) {
                            val xVd = response.header("X-VD").orEmpty()
                            if (xVd in SESSION_REJECTION_MARKERS) {
                                rejectedGeneration = maxOf(rejectedGeneration, state.generation)
                                rejectedMarker = xVd
                            }
                            analytics.log(LOG_TAG) {
                                "CDN failure code=${response.code} xVd=$xVd " +
                                    "closeRetry=$connectionCloseRetry ${state.summary()} " +
                                    // The only place a rejection's response headers are
                                    // visible. Set-Cookie in particular answers whether the
                                    // CDN is handing us a challenge cookie that the proxy's
                                    // cookie-less OkHttpClient would silently drop.
                                    "setCookie=${response.headers("Set-Cookie")} " +
                                    "target=${target.safeTarget()}"
                            }
                            null
                        } else {
                            backgroundRefreshConsumedSinceSuccess.set(false)
                            val rawBytes = response.body.bytes()
                            val bytes = if (response.code == 200 && isAssSubtitleUrl(target)) {
                                fixAssBytes(rawBytes)
                            } else {
                                rawBytes
                            }
                            FetchResult(
                                bytes = bytes,
                                statusCode = response.code,
                                contentType = response.header("Content-Type").orEmpty(),
                                finalUrl = response.request.url.toString(),
                                contentRange = response.header("Content-Range"),
                                acceptRanges = response.header("Accept-Ranges"),
                            )
                        }
                    }
                }.onFailure {
                    analytics.log(LOG_TAG) {
                        "CDN request failed type=${it::class.java.simpleName} " +
                            "message=${it.message} ${state.summary()} target=${target.safeTarget()}"
                    }
                }.getOrNull()

            perform(request, connectionCloseRetry = false)?.let { return it }
            if (statusCode != 403) return null

            requestPool.evictAll()
            analytics.log(LOG_TAG) {
                "Retrying exact 403 request with Connection: close ${state.summary()} " +
                    "target=${target.safeTarget()}"
            }
            val retryRequest = request.newBuilder().header("Connection", "close").build()
            val retryResult = perform(retryRequest, connectionCloseRetry = true)
            if (retryResult == null && statusCode == 403 && rejectedGeneration >= state.generation) {
                markSessionRejected(state, rejectedMarker)
            }
            return retryResult
        }

        val initialGeneration = initialState.generation

        // Range requests are excluded: byte-range semantics must stay exact. Playlist requests
        // ARE held (unlike the segment/range case there's no byte-continuity concern): without
        // this, a rejected playlist fetch fails immediately and Media3's poll loop just keeps
        // hitting the same still-invalid session until the *next* scheduled poll happens to land
        // after the refresh completes - holding gives the in-flight refresh a real chance to land
        // before this very request gives up.
        val canHoldForRefresh = !allowRange

        // Escalates to one background session refresh and, for plain segment requests, holds this
        // loopback request until the fresh token lands so it can be retried transparently.
        //
        // Holding is what lets Media3 ride the gap out of its own buffer: a pending HTTP request is
        // invisible to it, whereas the 503 serveSegment() would otherwise send trips the error path
        // immediately and surfaces as a visible loader.
        //
        // Skip the refresh entirely when the CDN rejected with token_decrypt: refresh() reloads the
        // SAME WebView instance, and logs show that always gets routed back to the identical
        // accepts-controls edge_hash it just failed with - so the retry is a guaranteed-useless
        // ~2-4s stall. Failing fast there lets the caller escalate straight to a genuinely fresh
        // WebView/extractor, which does get a new edge.
        suspend fun holdForRefreshThenRetry(): FetchResult? {
            if (lastRejectionMarker.get() == TOKEN_DECRYPT_MARKER) return null
            if (!scheduleBackgroundSessionRefresh(initialGeneration)) return null
            if (!canHoldForRefresh) return null
            val deadline = System.currentTimeMillis() + SEGMENT_HOLD_FOR_REFRESH_MS
            while (System.currentTimeMillis() < deadline) {
                delay(SESSION_REFRESH_POLL_MS)
                val state = streamStateProvider()
                if (state.generation > initialGeneration) {
                    noteActiveState(state)
                    analytics.log(LOG_TAG) {
                        "Retrying held segment on refreshed session ${state.summary()} " +
                            "target=${url.safeTarget()}"
                    }
                    // Prefer the current session's path: the refreshed master usually moves.
                    val target = rewriteToCurrentPath(url, state.masterUrl) ?: url
                    return execute(target)
                }
            }
            analytics.log(LOG_TAG) { "Held segment timed out waiting for refresh target=${url.safeTarget()}" }
            return null
        }

        if (rejectedSessionGeneration.get() >= initialGeneration) {
            return holdForRefreshThenRetry()
        }

        // Exact request first. A 403 receives exactly one same-URL retry on a fresh connection
        // inside execute(), matching the reference proxy.
        execute(url)?.let { return it }

        // A Media3 request may still point at the path of the preceding generation. Rewriting to
        // the currently authorized master path is cheap and remains synchronous.
        if (!isPlaylistUrl(url)) {
            rewriteToCurrentPath(url, streamStateProvider().masterUrl)?.let { rewritten ->
                analytics.log(LOG_TAG) { "Retrying segment on current session path target=${rewritten.safeTarget()}" }
                execute(rewritten)?.let { return it }
            }
        }

        return holdForRefreshThenRetry()
    }

    private fun markSessionRejected(state: AllohaStreamState, marker: String?) {
        while (true) {
            val previous = rejectedSessionGeneration.get()
            if (state.generation <= previous) return
            if (rejectedSessionGeneration.compareAndSet(previous, state.generation)) {
                lastRejectionMarker.set(marker)
                analytics.log(LOG_TAG) { "Session generation confirmed rejected marker=$marker ${state.summary()}" }
                return
            }
        }
    }

    /** The full fingerprinted state only when [verbose] is on - see the field's comment. */
    private fun AllohaStreamState.summary(): String =
        if (verbose) safeSummary() else shortSummary()

    private fun rewriteToCurrentPath(failedUrl: String, currentMaster: String): String? {
        if (currentMaster.isBlank()) return null
        val segmentName = failedUrl.substringAfterLast('/').substringBefore('?')
        if (segmentName.isBlank() || segmentName.equals(
                "master.m3u8",
                ignoreCase = true
            )
        ) return null
        val newBase = currentMaster.substringBeforeLast('/') + "/"
        val oldBase = failedUrl.substringBeforeLast('/') + "/"
        if (oldBase == newBase) return null
        return newBase + segmentName
    }

    private fun sendBytes(output: OutputStream, result: FetchResult) {
        val reason = if (result.statusCode == 206) "Partial Content" else "OK"
        val headers = buildString {
            append("HTTP/1.1 ${result.statusCode} $reason\r\n")
            append("Content-Type: ${result.contentType.ifBlank { "video/MP2T" }}\r\n")
            result.contentRange?.let { append("Content-Range: $it\r\n") }
            result.acceptRanges?.let { append("Accept-Ranges: $it\r\n") }
            append("Content-Length: ${result.bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }
        runCatching {
            output.write(headers.toByteArray())
            output.write(result.bytes)
            output.flush()
        }
    }

    private suspend fun refreshSessionOnce(previousGeneration: Long): Boolean {
        if (streamStateProvider().generation > previousGeneration) return true
        val isLeader = synchronized(sessionRefreshLock) {
            if (sessionRefreshInProgress) false else {
                sessionRefreshInProgress = true
                true
            }
        }
        if (isLeader) {
            try {
                analytics.log(LOG_TAG) { "Requesting session refresh generation=$previousGeneration" }
                requestSessionRefresh()
                val deadline = System.currentTimeMillis() + SESSION_REFRESH_WAIT_MS
                while (System.currentTimeMillis() < deadline) {
                    val state = streamStateProvider()
                    if (state.generation > previousGeneration) {
                        noteActiveState(state)
                        analytics.log(LOG_TAG) { "Session refresh completed ${state.summary()}" }
                        return true
                    }
                    delay(SESSION_REFRESH_POLL_MS)
                }
            } finally {
                synchronized(sessionRefreshLock) {
                    sessionRefreshInProgress = false
                }
            }
        } else {
            val deadline = System.currentTimeMillis() + SESSION_REFRESH_WAIT_MS
            while (sessionRefreshInProgress && System.currentTimeMillis() < deadline) {
                if (streamStateProvider().generation > previousGeneration) return true
                delay(SESSION_REFRESH_POLL_MS)
            }
        }
        return streamStateProvider().generation > previousGeneration
    }

    /** @return true when a refresh is now in flight, so a caller may wait for a new generation. */
    private fun scheduleBackgroundSessionRefresh(previousGeneration: Long): Boolean {
        if (!backgroundRefreshScheduled.compareAndSet(false, true)) {
            analytics.log(LOG_TAG) { "Background session refresh already scheduled generation=$previousGeneration" }
            return true
        }
        if (!backgroundRefreshConsumedSinceSuccess.compareAndSet(false, true)) {
            backgroundRefreshScheduled.set(false)
            analytics.log(LOG_TAG) {
                "Background session refresh already consumed without a successful CDN response " +
                    "generation=$previousGeneration"
            }
            return false
        }
        scope.launch {
            try {
                analytics.log(LOG_TAG) { "Starting background session refresh generation=$previousGeneration" }
                if (!refreshSessionOnce(previousGeneration)) {
                    analytics.log(LOG_TAG) { "Background session refresh timed out generation=$previousGeneration" }
                }
            } finally {
                backgroundRefreshScheduled.set(false)
            }
        }
        return true
    }

    private fun rewritePlaylist(content: String, playlistUrl: String): String {
        val base = URL(playlistUrl)
        val collectedSegments = mutableListOf<String>()
        fun proxy(value: String, trackForPrefetch: Boolean): String {
            val absolute = URL(base, value).toString()
            if (trackForPrefetch) collectedSegments += absolute
            val encoded =
                Base64.encodeToString(absolute.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
            return "http://$LOOPBACK_HOST:${server.localPort}/proxy?url=$encoded&token=$sessionToken"
        }

        val rewritten = content.lines().joinToString("\n") { line ->
            when {
                line.isBlank() -> line
                line.startsWith("#") -> line.replace(URI_ATTRIBUTE) {
                    "URI=\"${proxy(it.groupValues[1], trackForPrefetch = false)}\""
                }

                else -> proxy(line.trim(), trackForPrefetch = true)
            }
        }
        if (collectedSegments.isNotEmpty()) {
            recentSegments = collectedSegments
            scope.launch {
                collectedSegments.take(PREFETCH_COUNT).forEach { getOrFetch(it) }
            }
        }
        return rewritten
    }

    private fun logPlaylistAudioState(content: String) {
        val audioEntries = content.lineSequence()
            .map(String::trim)
            .filter { it.startsWith("#EXT-X-MEDIA:") && it.contains("TYPE=AUDIO") }
            .map { line ->
                AUDIO_LOG_ATTRIBUTE.findAll(line)
                    .associate { match ->
                        match.groupValues[1] to match.groupValues[2].trim('"')
                    }
                    .filterKeys { it in AUDIO_LOG_ATTRIBUTES }
            }
            .toList()
        analytics.log(LOG_TAG) {
            "playlist master=${content.contains("#EXT-X-STREAM-INF")} " +
                "audioEntries=${audioEntries.size} audio=$audioEntries " +
                "children=${
                    content.lineSequence().map(String::trim).filter { line ->
                        line.isNotBlank() && !line.startsWith("#") && line.contains(".m3u8")
                    }.map { it.substringBefore('?').substringAfterLast('/') }.toList()
                }"
        }
    }

    private fun send(output: OutputStream, code: Int, type: String, bytes: ByteArray) {
        val reason = when (code) {
            200 -> "OK"; 404 -> "Not Found"; else -> "Service Unavailable"
        }
        runCatching {
            output.write("HTTP/1.1 $code $reason\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            output.write(bytes)
            output.flush()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        scope.launch {
            try {
                runCatching { server.close() }
                client.dispatcher.cancelAll()
                client.connectionPool.evictAll()
                synchronized(cacheLock) { segmentCache.clear() }
                inFlightSegments.clear()
            } finally {
                scope.cancel()
            }
        }
    }

    private companion object {
        const val LOG_TAG = "AllohaStreamProxy"

        /** Both the bind address and the host of every URL handed to Media3 - they must match. */
        const val LOOPBACK_HOST = "127.0.0.1"
        const val SESSION_REFRESH_WAIT_MS = 20_000L
        const val SESSION_REFRESH_POLL_MS = 150L

        // How long a plain segment/playlist request may hang waiting for a session refresh before
        // we give up and let the caller fail it. Media3 plays out of its own buffer for the whole
        // hold, so this is affordable against the 15-60s it keeps (see PlayerLoadControlFactory).
        // It must outlast AllohaExtractor's forced staged-commit timeout (8s) with real margin -
        // not just nominally: IO-to-main-thread marshaling and SESSION_REFRESH_POLL_MS polling
        // granularity both eat into that budget on the request actually waiting here. 14s leaves
        // ~6s of margin over the 8s floor, while staying under PlayerDataSourceFactory's 16s HTTP
        // read timeout so the held loopback request itself never trips a client-side timeout.
        const val SEGMENT_HOLD_FOR_REFRESH_MS = 14_000L
        const val CACHE_CAPACITY = 4
        const val PREFETCH_COUNT = 2
        const val MIN_SEGMENT_BYTES_HINT = 1000
        val SUCCESS_CODES = setOf(200, 206)

        // Transport / hop-by-hop headers OkHttp must own itself, plus a few per-request headers that
        // must NOT be replayed. The CDN's token decryption keys off the stable anti-bot headers
        // (accepts-controls = rotating edge_hash, authorizations = per-movie token). "borth" is a
        // per-request nonce the player computes for its OWN fetch; replaying a stale/consumed borth
        // makes the CDN intermittently answer 403 xVd=token_decrypt (works only when it happens to
        // still be valid). The reference parser omits borth/x-requested-with/content-type entirely
        // and streams reliably, so we mirror that. "cookie" is applied separately from CookieManager.
        val BLOCKED_FORWARD_HEADERS = setOf(
            "host",
            "connection",
            "content-length",
            "transfer-encoding",
            "accept-encoding",
            "cookie",
            "borth",
            "x-requested-with",
            "content-type",
        )
        const val DEFAULT_ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"
        val CHROME_VERSION = Regex("""Chrome/(\d+)""")

        fun platformHintFor(userAgent: String): String = when {
            userAgent.contains("Windows", ignoreCase = true) -> "\"Windows\""
            userAgent.contains("Macintosh", ignoreCase = true) -> "\"macOS\""
            else -> "\"Linux\""
        }

        const val TOKEN_DECRYPT_MARKER = "token_decrypt"
        val SESSION_REJECTION_MARKERS =
            setOf("session_blocked", TOKEN_DECRYPT_MARKER, "client_blocked")
        val EMPTY_TS_PACKET = ByteArray(188).also {
            it[0] = 0x47
            it[1] = 0x1F.toByte()
            it[2] = 0xFF.toByte()
            it[3] = 0x10
        }
        val URI_ATTRIBUTE = Regex("""URI="([^"]+)""", RegexOption.IGNORE_CASE)
        val AUDIO_LOG_ATTRIBUTE = Regex("""([A-Z-]+)=((?:"[^"]*")|[^,]*)""")
        val AUDIO_LOG_ATTRIBUTES = setOf("GROUP-ID", "NAME", "LANGUAGE", "DEFAULT", "AUTOSELECT")

        fun contentTypeFor(url: String): String = when {
            url.contains(".vtt", true) -> "text/vtt"
            url.contains(".ass", true) || url.contains(".ssa", true) -> "text/x-ssa"
            url.contains(".aac", true) -> "audio/aac"
            url.contains(".m4s", true) || url.contains(".mp4", true) -> "video/mp4"
            else -> "video/MP2T"
        }

        fun isPlaylistUrl(url: String): Boolean = url.contains(".m3u8", ignoreCase = true)

        fun String.isHttpUrl(): Boolean =
            startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

        /** Splits a request path's query into decoded name/value pairs in a single pass. */
        fun parseQuery(path: String): Map<String, String> {
            val query = path.substringAfter('?', "")
            if (query.isEmpty()) return emptyMap()
            return buildMap {
                query.splitToSequence('&').forEach { pair ->
                    val separator = pair.indexOf('=')
                    if (separator <= 0) return@forEach
                    val value = runCatching {
                        URLDecoder.decode(pair.substring(separator + 1), "UTF-8")
                    }.getOrNull() ?: return@forEach
                    put(pair.substring(0, separator), value)
                }
            }
        }

        fun isAssSubtitleUrl(url: String): Boolean {
            val extension = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            return extension == "ass" || extension == "ssa"
        }

        /** Falls back to the original bytes untouched if the content isn't parseable as ASS/SSA text. */
        fun fixAssBytes(bytes: ByteArray): ByteArray = runCatching {
            fixAssMarginPositions(bytes.toString(Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
        }.getOrDefault(bytes)

        fun String?.fingerprint(): String {
            if (this.isNullOrBlank()) return "none"
            return MessageDigest.getInstance("SHA-256")
                .digest(toByteArray())
                .take(4)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }

        fun AllohaStreamState.shortSummary(): String {
            val host = runCatching { URL(masterUrl).host }.getOrDefault("unknown")
            return "generation=$generation host=$host"
        }

        fun AllohaStreamState.safeSummary(): String {
            val host = runCatching { URL(masterUrl).host }.getOrDefault("unknown")
            val ttlSeconds =
                expiresAtMs?.let { ((it - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L) }
            val cookie = sequenceOf(masterUrl, headers["origin"], headers["referer"])
                .filterNotNull()
                .mapNotNull {
                    runCatching { CookieManager.getInstance().getCookie(it) }.getOrNull()
                }
                .firstOrNull(String::isNotBlank)
            return "generation=$generation host=$host ttl=${ttlSeconds ?: "none"} " +
                "auth=${headers["authorizations"].fingerprint()} " +
                "controls=${headers["accepts-controls"].fingerprint()} " +
                "ua=${headers["user-agent"].fingerprint()} " +
                "cookie=${cookie.fingerprint()}"
        }

        fun String.safeTarget(): String = runCatching {
            val parsed = URL(this)
            "${parsed.host}/${parsed.path.substringAfterLast('/')}"
        }.getOrDefault(substringAfterLast('/').substringBefore('?'))
    }
}

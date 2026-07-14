package su.afk.yummy.tv.data.player.extractor

import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
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
 *     stale keep-alive connections survive a session refresh.
 *  5. Serving the reference 188-byte empty TS packet for unrecoverable `-a1.ts`/`-a2.ts` audio
 *     segments, while every other failed media segment remains an explicit transport error.
 *  6. A single-flight escalation to one full session refresh after bounded local recovery.
 */
internal class AllohaStreamProxy(
    private val streamStateProvider: () -> AllohaStreamState,
    private val qualityMasterProvider: (String) -> String?,
    private val requestSessionRefresh: () -> Unit,
) : AutoCloseable {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val server = ServerSocket(0, 8)

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

    val playbackUrl = "http://127.0.0.1:${server.localPort}/master.m3u8"

    init {
        Log.i(
            LOG_TAG,
            "Proxy started port=${server.localPort} ${streamStateProvider().safeSummary()}",
        )
        scope.launch {
            while (isActive) {
                runCatching { server.accept() }.getOrNull()
                    ?.let { socket -> launch { runCatching { handle(socket) } } }
            }
        }
    }

    fun qualityUrl(label: String): String =
        "$playbackUrl?quality=${URLEncoder.encode(label, "UTF-8")}"

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
        val oldClient = client
        val oldPool = connectionPool
        connectionPool = buildConnectionPool()
        client = buildClient()
        synchronized(cacheLock) { segmentCache.clear() }
        recentSegments = emptyList()
        val oldInFlight = inFlightSegments.values.toList()
        inFlightSegments.clear()
        oldInFlight.forEach { it.cancel() }
        oldClient.dispatcher.cancelAll()
        oldPool.evictAll()
        Log.i(LOG_TAG, "Session transport reset ${state.safeSummary()}")
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
            val state = streamStateProvider()
            noteActiveState(state)
            val path = requestLine.split(' ').getOrNull(1) ?: return
            val target = if (path.startsWith("/master.m3u8")) {
                val quality = path.substringAfter("quality=", "")
                    .takeIf(String::isNotBlank)
                    ?.let { encoded -> URLDecoder.decode(encoded.substringBefore('&'), "UTF-8") }
                quality?.let(qualityMasterProvider) ?: state.masterUrl
            } else {
                path.substringAfter("url=", "").takeIf(String::isNotBlank)
                    ?.let { encoded ->
                        String(
                            Base64.decode(
                                URLDecoder.decode(encoded, "UTF-8"),
                                Base64.URL_SAFE
                            )
                        )
                    }
                    .orEmpty()
            }
            if (target.isBlank()) {
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
        logPlaylistAudioState(text)
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
                Log.w(
                    LOG_TAG,
                    "Audio TS segment unrecoverable, serving empty packet: ${url.takeLast(80)}"
                )
                sendBytes(output, FetchResult(EMPTY_TS_PACKET, 200, "video/MP2T", url))
            } else {
                Log.w(LOG_TAG, "Media segment unrecoverable: ${url.takeLast(80)}")
                send(output, 503, "text/plain", byteArrayOf())
            }
            return
        }
        if (result.bytes.size < MIN_SEGMENT_BYTES_HINT) {
            Log.w(
                LOG_TAG,
                "Serving suspiciously small segment (${result.bytes.size}b): ${url.takeLast(80)}"
            )
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
            // A generation rotation deliberately cancels every old in-flight deferred. The
            // loopback caller still needs a complete HTTP response, so retry this one segment
            // against the already-committed generation instead of letting the socket end at EOF.
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
        if (rejectedSessionGeneration.get() >= initialState.generation) {
            if (lastRejectionMarker.get() != TOKEN_DECRYPT_MARKER) {
                scheduleBackgroundSessionRefresh(initialState.generation)
            }
            return null
        }

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
                cookie?.let { header("Cookie", it) }
                if (forwardRange) {
                    requestHeaders["range"]?.let { header("Range", it) }
                    requestHeaders["if-range"]?.let { header("If-Range", it) }
                }
            }.build()

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
                            Log.w(
                                LOG_TAG,
                                "CDN failure code=${response.code} xVd=$xVd " +
                                        "closeRetry=$connectionCloseRetry ${state.safeSummary()} " +
                                        "target=${target.safeTarget()}",
                            )
                            null
                        } else {
                            backgroundRefreshConsumedSinceSuccess.set(false)
                            FetchResult(
                                bytes = response.body.bytes(),
                                statusCode = response.code,
                                contentType = response.header("Content-Type").orEmpty(),
                                finalUrl = response.request.url.toString(),
                                contentRange = response.header("Content-Range"),
                                acceptRanges = response.header("Accept-Ranges"),
                            )
                        }
                    }
                }.onFailure {
                    Log.w(
                        LOG_TAG,
                        "CDN request failed type=${it::class.java.simpleName} " +
                                "message=${it.message} ${state.safeSummary()} target=${target.safeTarget()}"
                    )
                }.getOrNull()

            perform(request, connectionCloseRetry = false)?.let { return it }
            if (statusCode != 403) return null

            requestPool.evictAll()
            Log.i(
                LOG_TAG,
                "Retrying exact 403 request with Connection: close ${state.safeSummary()} " +
                        "target=${target.safeTarget()}",
            )
            val retryRequest = request.newBuilder().header("Connection", "close").build()
            val retryResult = perform(retryRequest, connectionCloseRetry = true)
            if (retryResult == null && statusCode == 403 && rejectedGeneration >= state.generation) {
                markSessionRejected(state, rejectedMarker)
            }
            return retryResult
        }

        val initialGeneration = initialState.generation

        // Exact request first. A 403 receives exactly one same-URL retry on a fresh connection
        // inside execute(), matching the reference proxy.
        execute(url)?.let { return it }

        // A Media3 request may still point at the path of the preceding generation. Rewriting to
        // the currently authorized master path is cheap and remains synchronous.
        if (!isPlaylistUrl(url)) {
            rewriteToCurrentPath(url, streamStateProvider().masterUrl)?.let { rewritten ->
                Log.i(
                    LOG_TAG,
                    "Retrying segment on current session path target=${rewritten.safeTarget()}",
                )
                execute(rewritten)?.let { return it }
            }
        }

        // Do not hold Media3's localhost segment request while WebView establishes a new session.
        // All concurrent failures share this one background refresh and receive an immediate
        // 188-byte audio fallback or 503 from serveSegment(). Skip the refresh entirely when the CDN
        // rejected with token_decrypt: refresh() reloads the SAME WebView instance, and logs show
        // that always gets routed back to the identical accepts-controls edge_hash it just failed
        // with - so the retry is a guaranteed-useless ~2-4s stall. Failing fast here lets the caller
        // escalate straight to a genuinely fresh WebView/extractor, which does get a new edge.
        if (lastRejectionMarker.get() != TOKEN_DECRYPT_MARKER) {
            scheduleBackgroundSessionRefresh(initialGeneration)
        }
        return null
    }

    private fun markSessionRejected(state: AllohaStreamState, marker: String?) {
        while (true) {
            val previous = rejectedSessionGeneration.get()
            if (state.generation <= previous) return
            if (rejectedSessionGeneration.compareAndSet(previous, state.generation)) {
                lastRejectionMarker.set(marker)
                Log.w(
                    LOG_TAG,
                    "Session generation confirmed rejected marker=$marker ${state.safeSummary()}"
                )
                return
            }
        }
    }

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
                Log.i(LOG_TAG, "Requesting session refresh generation=$previousGeneration")
                requestSessionRefresh()
                val deadline = System.currentTimeMillis() + SESSION_REFRESH_WAIT_MS
                while (System.currentTimeMillis() < deadline) {
                    val state = streamStateProvider()
                    if (state.generation > previousGeneration) {
                        noteActiveState(state)
                        Log.i(
                            LOG_TAG,
                            "Session refresh completed ${state.safeSummary()}",
                        )
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

    private fun scheduleBackgroundSessionRefresh(previousGeneration: Long) {
        if (!backgroundRefreshScheduled.compareAndSet(false, true)) {
            Log.d(
                LOG_TAG,
                "Background session refresh already scheduled generation=$previousGeneration"
            )
            return
        }
        if (!backgroundRefreshConsumedSinceSuccess.compareAndSet(false, true)) {
            backgroundRefreshScheduled.set(false)
            Log.w(
                LOG_TAG,
                "Background session refresh already consumed without a successful CDN response " +
                        "generation=$previousGeneration",
            )
            return
        }
        scope.launch {
            try {
                Log.i(LOG_TAG, "Starting background session refresh generation=$previousGeneration")
                if (!refreshSessionOnce(previousGeneration)) {
                    Log.w(
                        LOG_TAG,
                        "Background session refresh timed out generation=$previousGeneration"
                    )
                }
            } finally {
                backgroundRefreshScheduled.set(false)
            }
        }
    }

    private fun rewritePlaylist(content: String, playlistUrl: String): String {
        val base = URL(playlistUrl)
        val collectedSegments = mutableListOf<String>()
        fun proxy(value: String, trackForPrefetch: Boolean): String {
            val absolute = URL(base, value).toString()
            if (trackForPrefetch) collectedSegments += absolute
            val encoded =
                Base64.encodeToString(absolute.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
            return "http://127.0.0.1:${server.localPort}/proxy?url=$encoded"
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
        Log.i(
            LOG_TAG,
            "playlist master=${content.contains("#EXT-X-STREAM-INF")} " +
                    "audioEntries=${audioEntries.size} audio=$audioEntries " +
                    "children=${
                        content.lineSequence().map(String::trim).filter { line ->
                            line.isNotBlank() && !line.startsWith("#") && line.contains(".m3u8")
                        }.map { it.substringBefore('?').substringAfterLast('/') }.toList()
                    }",
        )
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
        const val SESSION_REFRESH_WAIT_MS = 20_000L
        const val SESSION_REFRESH_POLL_MS = 150L
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
            url.contains(".aac", true) -> "audio/aac"
            url.contains(".m4s", true) || url.contains(".mp4", true) -> "video/mp4"
            else -> "video/MP2T"
        }

        fun isPlaylistUrl(url: String): Boolean = url.contains(".m3u8", ignoreCase = true)

        fun String?.fingerprint(): String {
            if (this.isNullOrBlank()) return "none"
            return MessageDigest.getInstance("SHA-256")
                .digest(toByteArray())
                .take(4)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
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

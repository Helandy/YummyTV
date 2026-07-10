package su.afk.yummy.tv.data.player.extractor

import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class AllohaStreamProxy(
    private val streamStateProvider: () -> Pair<Map<String, String>, String>,
    private val qualityMasterProvider: (String) -> String?,
    private val fallbackMasterProvider: () -> String,
    private val requestSessionRefresh: () -> Unit,
) : AutoCloseable {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val server = ServerSocket(0, 8)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val sessionRefreshLock = Any()
    private val closed = AtomicBoolean(false)

    @Volatile
    private var sessionRefreshInProgress = false

    val playbackUrl = "http://127.0.0.1:${server.localPort}/master.m3u8"

    init {
        scope.launch {
            while (isActive) {
                runCatching { server.accept() }.getOrNull()
                    ?.let { socket -> launch { runCatching { handle(socket) } } }
            }
        }
    }

    fun qualityUrl(label: String): String =
        "$playbackUrl?quality=${URLEncoder.encode(label, "UTF-8")}"

    private fun handle(socket: Socket) {
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
            val target = if (path.startsWith("/master.m3u8")) {
                val quality = path.substringAfter("quality=", "")
                    .takeIf(String::isNotBlank)
                    ?.let { URLDecoder.decode(it.substringBefore('&'), "UTF-8") }
                quality?.let(qualityMasterProvider) ?: streamStateProvider().second
            } else {
                path.substringAfter("url=", "").takeIf(String::isNotBlank)
                    ?.let { String(Base64.decode(URLDecoder.decode(it, "UTF-8"), Base64.URL_SAFE)) }
                    .orEmpty()
            }
            if (target.isBlank()) return send(
                it.getOutputStream(),
                404,
                "text/plain",
                byteArrayOf()
            )
            serve(target, requestHeaders, it.getOutputStream())
        }
    }

    private fun serve(url: String, requestHeaders: Map<String, String>, output: OutputStream) {
        val response = fetch(url, requestHeaders) ?: return send(
            output,
            503,
            "text/plain",
            byteArrayOf(),
        )
        response.use {
            val contentType = it.header("Content-Type").orEmpty()
            if (url.contains(".m3u8", true) || contentType.contains("mpegurl", true)) {
                val playlist = it.body.string()
                logPlaylistAudioState(playlist)
                send(
                    output,
                    200,
                    "application/vnd.apple.mpegurl",
                    rewritePlaylist(playlist, it.request.url.toString()).toByteArray(),
                )
            } else {
                streamResponse(
                    output = output,
                    response = it,
                    contentType = contentType.ifBlank { contentTypeFor(url) },
                )
            }
        }
    }

    private fun fetch(url: String, requestHeaders: Map<String, String>): Response? {
        var requiresNewSession = false
        var refreshRequested = false
        fun execute(target: String, allowRange: Boolean = true): Response? {
            val (currentHeaders, _) = streamStateProvider()
            val request = Request.Builder().url(target).apply {
                currentHeaders.forEach { (name, value) ->
                    if (name.lowercase() in FORWARDED_HEADERS &&
                        !name.equals("cookie", ignoreCase = true)
                    ) header(name, value)
                }
                sequenceOf(
                    target,
                    currentHeaders["origin"],
                    currentHeaders["referer"],
                ).filterNotNull()
                    .mapNotNull {
                        runCatching {
                            CookieManager.getInstance().getCookie(it)
                        }.getOrNull()
                    }
                    .firstOrNull(String::isNotBlank)
                    ?.let { header("Cookie", it) }
                if (allowRange) {
                    requestHeaders["range"]?.let { header("Range", it) }
                    requestHeaders["if-range"]?.let { header("If-Range", it) }
                }
            }.build()
            return runCatching {
                client.newCall(request).execute().let { response ->
                    if (response.code !in SUCCESS_CODES) {
                        val xVd = response.header("X-VD").orEmpty()
                        if (xVd == "session_blocked" || xVd == "token_decrypt") {
                            requiresNewSession = true
                        }
                        Log.w(
                            LOG_TAG,
                            "CDN failure code=${response.code} xVd=$xVd " +
                                    "headers=${currentHeaders.keys.sorted()}",
                        )
                        response.close()
                        return@let null
                    }
                    response
                }
            }.onFailure {
                Log.w(
                    LOG_TAG,
                    "CDN request failed type=${it::class.java.simpleName} message=${it.message}"
                )
            }.getOrNull()
        }

        fun playlistBody(target: String): String? =
            execute(target, allowRange = false)?.use { it.body.string() }

        fun recoverFromFreshPlaylist(failedUrl: String): Response? {
            val segmentName = failedUrl.substringAfterLast('/').substringBefore('?')
            if (segmentName.isBlank()) return null
            val master = streamStateProvider().second.takeIf(String::isNotBlank) ?: return null
            val masterBody = playlistBody(master) ?: return null

            fun findInPlaylist(playlistUrl: String, body: String): String? =
                body.lineSequence()
                    .map(String::trim)
                    .firstOrNull { line ->
                        !line.startsWith("#") && line.isNotBlank() &&
                                line.substringAfterLast('/').substringBefore('?') == segmentName
                    }
                    ?.let { URL(URL(playlistUrl), it).toString() }

            findInPlaylist(master, masterBody)?.let { return execute(it) }
            val variantPath = masterBody.lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                ?: return null
            val variantUrl = URL(URL(master), variantPath).toString()
            val variantBody = playlistBody(variantUrl) ?: return null
            return findInPlaylist(variantUrl, variantBody)?.let(::execute)
        }
        repeat(FETCH_RECOVERY_ATTEMPTS) { attempt ->
            if (attempt >= 2 && !url.contains(".m3u8", ignoreCase = true)) {
                recoverFromFreshPlaylist(url)?.let { return it }
            }
            val currentMaster = streamStateProvider().second
            val fallbackMaster = fallbackMasterProvider()
            val fileName = url.substringAfterLast('/').substringBefore('?')
            val target = when {
                attempt == 0 -> url
                attempt == FETCH_RECOVERY_ATTEMPTS - 1 && fallbackMaster.isNotBlank() &&
                        fileName.equals("master.m3u8", ignoreCase = true) -> fallbackMaster

                fileName.equals("master.m3u8", ignoreCase = true) -> currentMaster
                currentMaster.isBlank() -> url
                attempt == FETCH_RECOVERY_ATTEMPTS - 1 && fallbackMaster.isNotBlank() ->
                    fallbackMaster.substringBeforeLast('/') + "/" + url.substringAfterLast('/')

                else -> currentMaster.substringBeforeLast('/') + "/" + url.substringAfterLast('/')
            }
            target.takeIf(String::isNotBlank)?.let(::execute)?.let { return it }
            if (!refreshRequested && (attempt == 0 || requiresNewSession)) {
                requiresNewSession = false
                refreshRequested = true
                refreshSessionOnce(streamStateProvider().second)
            }
            if (attempt < FETCH_RECOVERY_ATTEMPTS - 1) Thread.sleep(FETCH_RECOVERY_DELAY_MS)
        }
        return null
    }

    private fun streamResponse(output: OutputStream, response: Response, contentType: String) {
        val code = response.code
        val reason = if (code == 206) "Partial Content" else "OK"
        val headers = buildString {
            append("HTTP/1.1 $code $reason\r\n")
            append("Content-Type: $contentType\r\n")
            response.header("Content-Length")?.let { append("Content-Length: $it\r\n") }
            response.header("Content-Range")?.let { append("Content-Range: $it\r\n") }
            response.header("Accept-Ranges")?.let { append("Accept-Ranges: $it\r\n") }
            append("Connection: close\r\n\r\n")
        }
        output.write(headers.toByteArray())
        response.body.byteStream().use { input ->
            input.copyTo(output, STREAM_BUFFER_BYTES)
        }
        output.flush()
    }

    private fun refreshSessionOnce(previousMaster: String) {
        val isLeader = synchronized(sessionRefreshLock) {
            if (sessionRefreshInProgress) false else {
                sessionRefreshInProgress = true
                true
            }
        }
        if (isLeader) {
            try {
                requestSessionRefresh()
                val deadline = System.currentTimeMillis() + SESSION_REFRESH_WAIT_MS
                while (System.currentTimeMillis() < deadline) {
                    val currentMaster = streamStateProvider().second
                    if (currentMaster.isNotBlank() && currentMaster != previousMaster) break
                    Thread.sleep(SESSION_REFRESH_POLL_MS)
                }
            } finally {
                synchronized(sessionRefreshLock) {
                    sessionRefreshInProgress = false
                }
            }
        } else {
            val deadline = System.currentTimeMillis() + SESSION_REFRESH_WAIT_MS
            while (sessionRefreshInProgress && System.currentTimeMillis() < deadline) {
                Thread.sleep(SESSION_REFRESH_POLL_MS)
            }
        }
    }

    private fun rewritePlaylist(content: String, playlistUrl: String): String {
        val base = URL(playlistUrl)
        fun proxy(value: String): String {
            val absolute = URL(base, value).toString()
            val encoded =
                Base64.encodeToString(absolute.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
            return "http://127.0.0.1:${server.localPort}/proxy?url=$encoded"
        }
        return content.lines().joinToString("\n") { line ->
            when {
                line.isBlank() -> line
                line.startsWith("#") -> line.replace(URI_ATTRIBUTE) { "URI=\"${proxy(it.groupValues[1])}\"" }
                else -> proxy(line.trim())
            }
        }
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
            } finally {
                scope.cancel()
            }
        }
    }

    private companion object {
        const val LOG_TAG = "AllohaStreamProxy"
        const val FETCH_RECOVERY_ATTEMPTS = 3
        const val FETCH_RECOVERY_DELAY_MS = 500L
        const val SESSION_REFRESH_WAIT_MS = 8_000L
        const val SESSION_REFRESH_POLL_MS = 100L
        const val STREAM_BUFFER_BYTES = 64 * 1024
        val SUCCESS_CODES = setOf(200, 206)
        val FORWARDED_HEADERS = setOf(
            "accept",
            "accept-encoding",
            "accept-language",
            "authorizations",
            "accepts-controls",
            "origin",
            "referer",
            "sec-fetch-dest",
            "sec-fetch-mode",
            "sec-fetch-site",
            "sec-gpc",
            "dnt",
            "user-agent",
            "range",
        )
        val URI_ATTRIBUTE = Regex("""URI="([^"]+)""", RegexOption.IGNORE_CASE)
        val AUDIO_LOG_ATTRIBUTE = Regex("""([A-Z-]+)=((?:"[^"]*")|[^,]*)""")
        val AUDIO_LOG_ATTRIBUTES = setOf("GROUP-ID", "NAME", "LANGUAGE", "DEFAULT", "AUTOSELECT")
        fun contentTypeFor(url: String): String = when {
            url.contains(".vtt", true) -> "text/vtt"
            url.contains(".aac", true) -> "audio/aac"
            url.contains(".m4s", true) || url.contains(".mp4", true) -> "video/mp4"
            else -> "video/MP2T"
        }
    }
}

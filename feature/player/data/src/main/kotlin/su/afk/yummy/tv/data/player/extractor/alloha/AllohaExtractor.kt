package su.afk.yummy.tv.data.player.extractor.alloha

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.ioScope
import su.afk.yummy.tv.data.player.extractor.SessionAwarePlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.alloha.AllohaExtractor.Companion.MASTER_WAIT_TIMEOUT_MS
import su.afk.yummy.tv.data.player.extractor.alloha.AllohaExtractor.Companion.NO_SIGNAL_TIMEOUT_MS
import su.afk.yummy.tv.data.player.extractor.alloha.AllohaExtractor.Companion.TIMEOUT_MS
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.core.utils.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Opens Alloha's signed HLS session by loading the player page in a hidden WebView and observing
 * its own network stack. Alloha is the only balancer that never hands out a playable URL: what it
 * gives is a live, signed session that has to be captured, kept alive and proxied. See
 * `docs/alloha-player.md` for the end-to-end picture.
 *
 * This class is the orchestrator; the actual work is split up as:
 *  - [wrapperHtml] injects the JS that hooks the page's XHR/fetch/WebSocket and calls back in
 *    through [BRIDGE_NAME].
 *  - [parseSources] turns the captured `bnsi` payload into the dubbing/quality ladder.
 *  - [LiveAllohaStreamSession] owns the resulting live state and its rotation.
 *  - [AllohaStreamProxy] serves it to Media3 over loopback with this session's headers.
 *
 * A session is delivered once BOTH the parsed sources and a correctly signed master URL have
 * arrived. The three timeouts guarding that wait are deliberately layered - see
 * [NO_SIGNAL_TIMEOUT_MS], [MASTER_WAIT_TIMEOUT_MS] and [TIMEOUT_MS].
 */
internal class AllohaExtractor @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : SessionAwarePlayerStreamExtractor {
    private val extractorScope = ioScope()
    private val webViewPool = AllohaWebViewPool()

    // Picked once and then reused for every session. It used to be re-randomised on each open, so
    // a viewer who entered a few episodes in a row presented the site a fresh browser identity
    // (6 desktop OSes x 6 Chrome versions) every time from one address - which is exactly what a
    // bot farm looks like. The fingerprint has to be stable across sessions for the same reason the
    // reload path below keeps it stable within one.
    private val sessionUserAgent: String by lazy { desktopUserAgent() }

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

    override suspend fun openSession(
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
        var wrapperReloads = 0
        var reloadWrapper: (() -> Unit)? = null
        val liveSession = LiveAllohaStreamSession(handler, iframeUrl, analyticsTracker)
        val hostChangeFallback = Runnable {
            if (pendingHostChangeMaster != null) {
                // No fresh config_update confirmed the new host in time - the held headers are
                // still signed for the OLD host, so applying them would likely just 403. Force a
                // full session restart instead, same as the reference implementation does here.
                pendingHostChangeMaster = null
                analyticsTracker.log(LOG_TAG) { "host-change config_update timed out, forcing session restart" }
                liveSession.refresh()
            }
        }

        // Starting the proxy binds a ServerSocket and builds an OkHttp client, so it stays off the
        // main thread. The `delivered` flag and the timeout cleanup still happen on the handler,
        // which is what keeps this exclusive with fail()/the timeouts.
        fun resumeWithProxy() {
            extractorScope.launch {
                liveSession.startProxy()
                if (continuation.isActive) {
                    continuation.resume(AllohaOpenResult.Ready(liveSession))
                } else {
                    // Cancelled while the proxy was coming up - nothing will close it for us.
                    liveSession.close()
                }
            }
        }

        lateinit var timeout: Runnable
        lateinit var noSignalTimeout: Runnable
        val masterWaitTimeout = Runnable {
            if (!delivered && streamReady) {
                analyticsTracker.log(LOG_TAG) { "refreshed master timeout, using captured quality playlist" }
                delivered = true
                handler.removeCallbacks(timeout)
                resumeWithProxy()
            }
        }

        fun deliverWhenReady() {
            if (delivered || !streamReady || !refreshedMasterReady) return
            delivered = true
            handler.removeCallbacks(timeout)
            handler.removeCallbacks(masterWaitTimeout)
            handler.removeCallbacks(noSignalTimeout)
            resumeWithProxy()
        }

        fun fail(reason: AllohaOpenResult = AllohaOpenResult.Failed) {
            if (delivered) return
            delivered = true
            handler.removeCallbacksAndMessages(null)
            liveSession.close()
            if (continuation.isActive) continuation.resume(reason)
        }

        // Measured on device: when the iframe loads at all, onReady follows within 0.15-0.9s. A run
        // where nothing has arrived is therefore already lost long before TIMEOUT_MS - the page
        // silently never loads (no onload, no WebViewClient error), which is by far the most common
        // way re-entering the player hangs. Reloading the wrapper is cheap and usually succeeds, so
        // spend the same 30s budget on three chances instead of one dead wait.
        noSignalTimeout = Runnable {
            if (delivered || streamReady) return@Runnable
            if (wrapperReloads >= MAX_WRAPPER_RELOADS) return@Runnable
            wrapperReloads++
            analyticsTracker.log(LOG_TAG) {
                "no signal after ${NO_SIGNAL_TIMEOUT_MS}ms, reloading wrapper " +
                        "attempt=$wrapperReloads/$MAX_WRAPPER_RELOADS"
            }
            reloadWrapper?.invoke()
            handler.postDelayed(noSignalTimeout, NO_SIGNAL_TIMEOUT_MS)
        }
        handler.postDelayed(noSignalTimeout, NO_SIGNAL_TIMEOUT_MS)

        timeout = Runnable {
            // Which signal is missing IS the diagnosis for a cold-start hang: no streamReady means
            // the iframe never delivered its bnsi payload at all, while streamReady without a
            // master is normally absorbed by masterWaitTimeout long before this fires. Without this
            // line the 30s dead wait leaves nothing in logcat, only an analytics event.
            analyticsTracker.log(LOG_TAG) {
                "session timed out after ${TIMEOUT_MS}ms streamReady=$streamReady " +
                        "refreshedMaster=$refreshedMasterReady"
            }
            analyticsTracker.logExtractorFailure(
                "Alloha",
                iframeUrl,
                "timed out waiting for signed HLS session"
            )
            fail()
        }
        handler.postDelayed(timeout, TIMEOUT_MS)

        val bridge = object {
            @JavascriptInterface
            fun onReady(responseJson: String, headersJson: String) {
                extractorScope.launch {
                    val parsed = runCatching {
                        Pair(
                            parseSources(responseJson, analyticsTracker),
                            parseHeaders(headersJson)
                        )
                    }
                    handler.post {
                        parsed.onSuccess { (sources, headers) ->
                            CookieManager.getInstance().flush()
                            liveSession.initialize(sources, headers)
                            liveSession.preselectQuality(preferredQualityLabel)
                            liveSession.updateHeaders(headers)
                            fallbackTtlSeconds?.let(liveSession::ensureFallbackExpiry)
                            streamReady = true
                            handler.removeCallbacks(noSignalTimeout)
                            analyticsTracker.log(LOG_TAG) { "ready headers=${liveSession.safeHeaderState()}" }
                            deliverWhenReady()
                            if (!delivered) {
                                handler.removeCallbacks(masterWaitTimeout)
                                handler.postDelayed(masterWaitTimeout, MASTER_WAIT_TIMEOUT_MS)
                            }
                        }.onFailure {
                            analyticsTracker.logExtractorFailure(
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
                        analyticsTracker.log(LOG_TAG) { "host change confirmed by fresh config_update" }
                    }
                    analyticsTracker.log(LOG_TAG) { "config ttl=$ttlSeconds headers=${liveSession.safeHeaderState()}" }
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
                        if (liveSession.isRotating) {
                            // A staged rotation already gets this guarantee for free: its master and
                            // its edge_hash are applied to the live state in one step, and the
                            // previous token keeps serving until then. Holding the master back here
                            // would only stall the commit.
                            liveSession.updateMasterUrl(masterUrl)
                            analyticsTracker.log(LOG_TAG) { "master refreshed for staged rotation host=$newHost" }
                        } else if (refreshedMasterReady && previousHost != null && newHost != null &&
                            previousHost != newHost
                        ) {
                            // CDN node switched mid-session: the accepts-controls token we're
                            // still holding is signed for the OLD host and would 403 on the new
                            // one. Hold the master URL update until a fresh config_update confirms
                            // the new token, falling back to a full restart if none arrives.
                            //
                            // Unless this session has never received a config_update at all - with
                            // some CDNs the player's WebSocket dies seconds after opening and none
                            // ever arrives. Waiting then only ever ends in that same fallback, so
                            // take it straight away instead of stalling for HOST_CHANGE_CONFIG_WAIT_MS.
                            if (!liveSession.hasSeenConfigUpdate) {
                                analyticsTracker.log(LOG_TAG) {
                                    "master host changed $previousHost -> $newHost and no " +
                                            "config_update was ever seen, restarting session now"
                                }
                                liveSession.refresh()
                            } else {
                                pendingHostChangeMaster = masterUrl
                                handler.removeCallbacks(hostChangeFallback)
                                handler.postDelayed(hostChangeFallback, HOST_CHANGE_CONFIG_WAIT_MS)
                                analyticsTracker.log(LOG_TAG) { "master host changed $previousHost -> $newHost, awaiting fresh config_update" }
                            }
                        } else {
                            liveSession.updateMasterUrl(masterUrl)
                            refreshedMasterReady = true
                            analyticsTracker.log(LOG_TAG) { "master refreshed headers=${liveSession.safeHeaderState()}" }
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
                    analyticsTracker.logExtractorFailure(
                        "Alloha",
                        iframeUrl,
                        "site rendered a dubbing-unavailable message",
                    )
                    fail(AllohaOpenResult.Unavailable(message = null))
                }
            }

            @JavascriptInterface
            fun onLog(message: String) {
                analyticsTracker.log(LOG_TAG) { "WebView session: $message" }
            }
        }

        val userAgent = sessionUserAgent
        val parsedUrl = URL(iframeUrl)
        val baseUrl = "${parsedUrl.protocol}://${parsedUrl.host.lowercase(Locale.ROOT)}/"
        val html = wrapperHtml(iframeUrl)
        val webView = webViewPool.acquire(context, userAgent).apply {
            removeJavascriptInterface(BRIDGE_NAME)
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
        reloadWrapper = {
            webView.settings.userAgentString = userAgent
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
        liveSession.attach(
            webView = webView,
            refresh = {
                // A WebView reload rotates signed session data, but the browser fingerprint must
                // stay stable for the lifetime of this Alloha session.
                webView.settings.userAgentString = userAgent
                webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            },
            release = webViewPool::release,
        )

        continuation.invokeOnCancellation { handler.post { liveSession.close() } }
    }

    private fun String.hostOrNull(): String? =
        runCatching { URL(this).host }.getOrNull()?.takeIf(String::isNotBlank)

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
        const val TIMEOUT_MS = 30_000L

        // How long to wait with NO signal at all (no onReady) before reloading the wrapper. Kept
        // well above the observed 0.15-0.9s happy path so a genuinely slow connection still has
        // room, while MAX_WRAPPER_RELOADS keeps the total inside TIMEOUT_MS.
        const val NO_SIGNAL_TIMEOUT_MS = 10_000L
        const val MAX_WRAPPER_RELOADS = 2

        // How long to wait for the iframe's own master.m3u8 (onM3u8Refreshed) before falling back
        // to the raw bnsi quality URL. The bnsi URL carries a path token the CDN rejects with 403
        // token_decrypt once the live session is established; only the master the iframe itself
        // fetches is signed correctly, so the fallback must stay a genuine last resort. The wrapper
        // JS keeps the iframe player actively playing (even after the session is captured) so it
        // reliably (re)fetches that master within a couple of seconds; this window just needs a
        // little headroom over that.
        const val MASTER_WAIT_TIMEOUT_MS = 6_000L
        const val HOST_CHANGE_CONFIG_WAIT_MS = 10_000L
    }
}

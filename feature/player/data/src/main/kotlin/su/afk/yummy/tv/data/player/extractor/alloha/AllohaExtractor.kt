// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.data.player.extractor.alloha

import android.content.Context
import android.webkit.WebSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.common.player.AllohaEdgeHash
import su.afk.yummy.tv.core.network.di.AllohaOkHttp
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.isAllohaPlayerUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens Alloha's HLS session over plain HTTP - see [AllohaHttpSessionOpener] for the round trip and
 * `docs/alloha-player.md` for the whole picture. Alloha never hands out a URL in the site's
 * catalog; the ladder of CDN URLs comes from its `bnsi` call and plays with the session's headers,
 * which the player passes on as they are.
 *
 * This class only orchestrates: it opens the session with a retry and builds
 * a stream that carries every dubbing's ladder (see [buildAllohaStream]).
 */
@Singleton
internal class AllohaExtractor @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    @AllohaOkHttp private val httpClient: OkHttpClient,
) : PlayerStreamExtractor {
    @Volatile
    private var appContext: Context? = null

    // The system WebView's own user agent, read as a string (no WebView is created), like the site's player
    // in a WebView sends it. Picked once and reused for every session: re-randomising it on each open would
    // present the site a fresh browser identity from one address every time, which is what a bot farm looks like.
    private val sessionUserAgent: String by lazy {
        val context = appContext
        val system = context?.let { runCatching { WebSettings.getDefaultUserAgent(it) }.getOrNull() }
        system ?: FALLBACK_USER_AGENT
    }

    // Read once per process; the seed behind it is saved per install.
    private val clientProfile: AllohaClientProfile by lazy {
        allohaClientProfile(requireNotNull(appContext), sessionUserAgent)
    }

    // One playback at a time: opening a session replaces the previous socket.
    private var activeSocket: AllohaPlaybackSocket? = null
    private var activeReporter: AllohaViewReporter? = null

    private val opener: AllohaHttpSessionOpener by lazy {
        AllohaHttpSessionOpener(
            client = httpClient,
            userAgent = sessionUserAgent,
            fingerprint = clientProfile.fingerprint,
            analytics = analyticsTracker,
        )
    }

    override fun supports(url: String): Boolean = url.isAllohaPlayerUrl()

    override val reportsOwnProgress: Boolean = true

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult {
        val iframeUrl = request.iframeUrl
        appContext = context.applicationContext
        request.onStatus(PlayerStreamResolveStatus.OpeningSession)
        val opened = try {
            withContext(Dispatchers.IO) { openWithRetry(iframeUrl, request.onStatus) }
        } catch (error: AllohaSourceUnavailableException) {
            analyticsTracker.logExtractorFailure("Alloha", iframeUrl, error.message ?: "no source")
            // error.message is an internal debug reason (already logged above), not user-facing
            // text - the presentation layer supplies that.
            return PlayerStreamResolveResult.Unavailable(message = null)
        } catch (error: IOException) {
            analyticsTracker.logExtractorFailure("Alloha", iframeUrl, error.message ?: "invalid response")
            return PlayerStreamResolveResult.Failed("Alloha: session was not opened")
        }
        request.onStatus(PlayerStreamResolveStatus.RequestingStream)
        // A WebView adds the app package as X-Requested-With to its requests, media ones included.
        val identified = opened.copy(headers = opened.headers + ("x-requested-with" to context.packageName))
        return buildAllohaStream(withSocket(identified, iframeUrl, request.positionMs, request.durationMs), request.autoQualityLabel)
    }

    /**
     * Opens the player's WebSocket and puts the `edge_hash` it hands out into the media headers as
     * `Accepts-Controls`. A socket that does not answer leaves the session as it was: the raw links
     * played without it before.
     */
    private suspend fun withSocket(
        opened: AllohaOpenedSession,
        iframeUrl: String,
        positionMs: () -> Long,
        durationMs: () -> Long,
    ): AllohaOpenedSession {
        val ticket = opened.socketTicket ?: return opened
        activeSocket?.close()
        activeReporter?.close()
        activeReporter = null
        AllohaEdgeHash.current = null
        AllohaEdgeHash.bytesLoaded.set(0L)
        val track = opened.sources.audioTracks.let { tracks -> tracks.firstOrNull { it.isDefault } ?: tracks.first() }
        val resolution = track.qualities.keys.maxByOrNull { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            ?.filter(Char::isDigit).orEmpty().ifEmpty { "1080" }
        val socket = AllohaPlaybackSocket(
            client = httpClient,
            userAgent = sessionUserAgent,
            origin = opened.headers["origin"] ?: iframeUrl,
            analytics = analyticsTracker,
            positionMs = positionMs,
        )
        activeSocket = socket
        opened.viewTicket?.let { view ->
            val origin = opened.headers["origin"] ?: iframeUrl
            activeReporter = AllohaViewReporter(
                client = httpClient,
                ticket = view,
                stream = AllohaPlayedStream(
                    manifestUrl = track.qualities.entries.maxByOrNull {
                        it.key.filter(Char::isDigit).toIntOrNull() ?: 0
                    }?.value.orEmpty(),
                    audioLabel = track.label,
                    qualityHeight = resolution.toIntOrNull() ?: 0,
                ),
                iframeUrl = iframeUrl,
                origin = origin,
                userAgent = sessionUserAgent,
                profile = clientProfile,
                positionMs = positionMs,
                durationMs = durationMs,
                analytics = analyticsTracker,
            ).also { it.start() }
        }
        val hash = socket.open(ticket, resolution = resolution, audioId = track.id)
        analyticsTracker.log(LOG_TAG) { "ws first edge_hash=${hash != null}" }
        return if (hash == null) opened else opened.copy(headers = opened.headers + (AllohaEdgeHash.HEADER to hash))
    }

    /** One more attempt for a transient failure; a missing source is final and is not retried. */
    private suspend fun openWithRetry(
        iframeUrl: String,
        onStatus: (PlayerStreamResolveStatus) -> Unit,
    ): AllohaOpenedSession {
        var lastError: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                onStatus(PlayerStreamResolveStatus.Retrying)
                delay(RETRY_DELAY_MS)
            }
            try {
                onStatus(PlayerStreamResolveStatus.ParsingPage)
                return opener.open(iframeUrl)
            } catch (error: AllohaSourceUnavailableException) {
                throw error
            } catch (error: IOException) {
                analyticsTracker.log(LOG_TAG) { "open attempt ${attempt + 1}/$MAX_ATTEMPTS failed: ${error.message}" }
                lastError = error
            }
        }
        throw checkNotNull(lastError)
    }

    private companion object {
        const val LOG_TAG = PersistedLogTags.ALLOHA_EXTRACTOR

        // Only when the system WebView's user agent cannot be read.
        const val FALLBACK_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:159.0) Gecko/20100101 Firefox/159.0"
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 500L
    }
}

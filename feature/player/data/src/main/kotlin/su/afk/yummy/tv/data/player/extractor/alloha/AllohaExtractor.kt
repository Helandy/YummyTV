package su.afk.yummy.tv.data.player.extractor.alloha

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.isAllohaPlayerUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

/**
 * Opens Alloha's HLS session over plain HTTP - see [AllohaHttpSessionOpener] for the round trip and
 * `docs/alloha-player.md` for the whole picture. Alloha never hands out a URL in the site's
 * catalog; the ladder of CDN URLs comes from its `bnsi` call and plays with the session's headers,
 * which the player passes on as they are.
 *
 * This class only orchestrates: it opens the session with a retry and builds
 * a stream that carries every dubbing's ladder (see [buildAllohaStream]).
 */
internal class AllohaExtractor @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : PlayerStreamExtractor {
    // Picked once and reused for every session. Re-randomising it on each open would present the
    // site a fresh browser identity from one address every time, which is what a bot farm looks like.
    private val sessionUserAgent: String by lazy { desktopUserAgent() }

    private val opener: AllohaHttpSessionOpener by lazy {
        AllohaHttpSessionOpener(
            client = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build(),
            userAgent = sessionUserAgent,
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
        return buildAllohaStream(opened, request.autoQualityLabel)
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

    private fun desktopUserAgent(): String {
        val os = DESKTOP_OS.random()
        val version = Random.nextInt(130, 136)
        return "Mozilla/5.0 ($os) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$version.0.0.0 Safari/537.36"
    }

    private companion object {
        const val LOG_TAG = PersistedLogTags.ALLOHA_EXTRACTOR
        val DESKTOP_OS = listOf(
            "Windows NT 10.0; Win64; x64",
            "Windows NT 11.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "Macintosh; Intel Mac OS X 14_4_1",
            "X11; Linux x86_64",
            "X11; Ubuntu; Linux x86_64",
        )
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 15L
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 500L
    }
}

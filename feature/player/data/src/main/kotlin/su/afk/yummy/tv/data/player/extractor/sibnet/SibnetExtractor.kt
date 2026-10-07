package su.afk.yummy.tv.data.player.extractor.sibnet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.core.utils.player.isSibnetPlayerUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.common.hasKnownUrlScheme
import su.afk.yummy.tv.data.player.extractor.common.logExtractorFailure
import su.afk.yummy.tv.data.player.extractor.common.normalizeUrlScheme
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.model.PlayerStreamUnavailableCause
import java.net.URL
import javax.inject.Inject

internal class SibnetExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
    private val analyticsTracker: AnalyticsTracker,
    private val userAgents: BrowserUserAgentProvider,
) : PlayerStreamExtractor {

    override fun supports(url: String): Boolean = url.isSibnetPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: android.content.Context,
    ): PlayerStreamResolveResult = withContext(Dispatchers.IO) {
        val playerUrl = normalizeUrl(request.iframeUrl)
        try {
            val page = fetchPlayerPage(playerUrl)
            val streamUrl = extractStreamUrl(page, playerUrl) ?: run {
                analyticsTracker.logExtractorFailure(
                    "Sibnet",
                    playerUrl,
                    "MP4 source was not found"
                )
                return@withContext PlayerStreamResolveResult.Failed("MP4 source was not found")
            }

            PlayerStreamResolveResult.Stream(
                url = streamUrl,
                headers = mapOf(
                    "Referer" to playerUrl,
                    "Origin" to SIBNET_ORIGIN,
                    "User-Agent" to userAgents.userAgent,
                ),
            )
        } catch (e: SibnetAccessForbiddenException) {
            analyticsTracker.logExtractorFailure("Sibnet", playerUrl, "HTTP 403: access is forbidden")
            PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.AccessForbidden)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            analyticsTracker.logExtractorFailure(
                "Sibnet",
                playerUrl,
                "unexpected extractor error",
                e
            )
            PlayerStreamResolveResult.Failed("${e::class.java.simpleName}: ${e.message.orEmpty().take(40)}")
        }
    }

    private suspend fun fetchPlayerPage(playerUrl: String): String {
        val response = httpClient.getText(
            url = playerUrl,
            headers = mapOf(
                "Referer" to YANI_ORIGIN,
                "User-Agent" to userAgents.userAgent,
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            ),
        )
        if (response.statusCode == HTTP_FORBIDDEN) throw SibnetAccessForbiddenException()
        if (!response.isSuccess) {
            throw IllegalStateException("HTTP ${response.statusCode}")
        }
        return response.body
    }

    private fun extractStreamUrl(page: String, playerUrl: String): String? =
        STREAM_URL_PATTERNS
            .firstNotNullOfOrNull { pattern ->
                pattern.find(page)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let { normalizeUrl(it, playerUrl) }
            }
            ?.takeIf { it.isNotBlank() }

    private fun normalizeUrl(url: String, baseUrl: String = ""): String {
        val normalized = url.trim().trim('"', '\'').replace("\\/", "/")
        if (normalized.isBlank()) return ""

        return when {
            // Deliberately NOT wrapped in runCatching (pre-existing behaviour): a malformed
            // baseUrl here throws rather than silently falling back.
            normalized.hasKnownUrlScheme() -> normalizeUrlScheme(normalized)
            baseUrl.isNotBlank() -> URL(URL(baseUrl), normalized).toString()
            else -> "https://$normalized"
        }
    }

    /** Sibnet закрыл страницу плеера (403) для этой сети: видео не при чём, повтор не поможет. */
    private class SibnetAccessForbiddenException : Exception()

    private companion object {
        const val HTTP_FORBIDDEN = 403
        const val SIBNET_ORIGIN = "https://video.sibnet.ru"
        const val YANI_ORIGIN = "https://yani.tv/"

        val STREAM_URL_PATTERNS = listOf(
            Regex("""player\.src\(\s*\[\s*\{\s*src\s*:\s*["']([^"']+)"""),
            Regex("""<source[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )
    }
}

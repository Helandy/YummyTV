package su.afk.yummy.tv.data.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import su.afk.yummy.tv.domain.player.isSibnetPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URL
import javax.inject.Inject

internal class SibnetExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
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
                logExtractorFailure("Sibnet", playerUrl, "MP4 source was not found")
                return@withContext PlayerStreamResolveResult.Failed
            }

            PlayerStreamResolveResult.Stream(
                url = streamUrl,
                headers = mapOf(
                    "Referer" to playerUrl,
                    "Origin" to SIBNET_ORIGIN,
                    "User-Agent" to CHROME_UA,
                ),
            )
        } catch (e: Exception) {
            logExtractorFailure("Sibnet", playerUrl, "unexpected extractor error", e)
            PlayerStreamResolveResult.Failed
        }
    }

    private suspend fun fetchPlayerPage(playerUrl: String): String {
        val response = httpClient.getText(
            url = playerUrl,
            headers = mapOf(
                "Referer" to YANI_ORIGIN,
                "User-Agent" to CHROME_UA,
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            ),
        )
        if (!response.isSuccess) {
            throw IllegalStateException("HTTP ${response.statusCode}: ${response.body.take(80)}")
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
            normalized.startsWith("//") -> "https:$normalized"
            normalized.startsWith("http://") -> normalized.replaceFirst("http://", "https://")
            normalized.startsWith("https://") -> normalized
            baseUrl.isNotBlank() -> URL(URL(baseUrl), normalized).toString()
            else -> "https://$normalized"
        }
    }

    private companion object {
        const val SIBNET_ORIGIN = "https://video.sibnet.ru"
        const val YANI_ORIGIN = "https://yani.tv/"

        val STREAM_URL_PATTERNS = listOf(
            Regex("""player\.src\(\s*\[\s*\{\s*src\s*:\s*["']([^"']+)"""),
            Regex("""<source[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )
    }
}

package su.afk.yummy.tv.data.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import su.afk.yummy.tv.domain.player.isAksorPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URI
import javax.inject.Inject

internal data class AksorResult(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>?,
)

internal class AksorExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
) : PlayerStreamExtractor {

    private val PLAYER_ORIGIN = "https://player.aksor.tv"
    private val QUALITY_ORDER = listOf("q360", "q480", "q720", "q1080", "q2k", "q4k")

    override fun supports(url: String): Boolean = url.isAksorPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: android.content.Context,
    ): PlayerStreamResolveResult =
        extractStream(request.iframeUrl)?.toStream() ?: PlayerStreamResolveResult.Failed

    private suspend fun extractStream(iframeUrl: String): AksorResult? =
        withContext(Dispatchers.IO) {
            try {
                val playerUrl = normalizeUrl(iframeUrl)
                val hash = extractHash(playerUrl) ?: run {
                    return@withContext null
                }
                val headers = streamHeaders(playerUrl)
                val apiUrl = "$PLAYER_ORIGIN/api/video/$hash"

                val qualities = runCatching {
                    fetchJson(apiUrl, referer = playerUrl)
                        .optJSONObject("qualities")
                        ?.toQualityMap()
                        ?.takeIf { it.isNotEmpty() }
                }.getOrNull()

                val streamUrl = qualities?.values?.lastOrNull()
                    ?: fetchFallbackStreamUrl(playerUrl, hash)
                    ?: return@withContext null

                AksorResult(
                    url = streamUrl,
                    headers = headers,
                    qualities = qualities,
                )
            } catch (e: Exception) {
                logExtractorFailure("Aksor", iframeUrl, "unexpected extractor error", e)
                null
            }
        }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "https://")
            trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    private fun extractHash(url: String): String? {
        val segments = URI(url).path
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val videoIndex = segments.indexOf("video")
        val hash = if (videoIndex >= 0) {
            segments.getOrNull(videoIndex + 1)
        } else {
            segments.lastOrNull()
        }
        return hash?.takeIf { it.isNotBlank() }
    }

    private fun JSONObject.toQualityMap(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        QUALITY_ORDER.forEach { key ->
            val streamUrl = optString(key)
                .trim()
                .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            if (streamUrl != null) {
                map[qualityLabel(key)] = streamUrl.sanitizeStreamUrl()
            }
        }
        return map
    }

    // Aksor's CDN paths can contain literal spaces (e.g. dubbing folder names like
    // "JAM CLUB"). Browsers auto-encode these, but Android's HTTP stack sends them
    // as-is, which some CDN edges reject with a 404 - so encode them ourselves.
    private fun String.sanitizeStreamUrl(): String =
        if (contains(' ')) replace(" ", "%20") else this

    private fun qualityLabel(key: String): String = when (key) {
        "q2k" -> "2K"
        "q4k" -> "4K"
        else -> "${key.removePrefix("q")}p"
    }

    private fun streamHeaders(playerUrl: String): Map<String, String> = mapOf(
        "Referer" to playerUrl,
        "User-Agent" to CHROME_UA,
    )

    private suspend fun fetchFallbackStreamUrl(playerUrl: String, hash: String): String? {
        val html = fetchText(playerUrl, referer = "https://yani.tv/", accept = "text/html")
        extractMetaVideoUrl(html)?.let { return it.sanitizeStreamUrl() }

        val scriptUrls = Regex("""<script[^>]+src=["']([^"']+)["']""")
            .findAll(html)
            .map { it.groupValues[1] }
            .map { src ->
                when {
                    src.startsWith("//") -> "https:$src"
                    src.startsWith("/") -> "$PLAYER_ORIGIN$src"
                    src.startsWith("http") -> src
                    else -> "$PLAYER_ORIGIN/$src"
                }
            }
            .toList()

        val apiPath = scriptUrls.firstNotNullOfOrNull { scriptUrl ->
            runCatching {
                Regex("""["']([^"']*/api)["']""")
                    .find(fetchText(scriptUrl, referer = playerUrl, accept = "*/*"))
                    ?.groupValues
                    ?.get(1)
            }.onFailure { e ->
                logExtractorFailure("Aksor", scriptUrl, "failed to inspect player script", e)
            }.getOrNull()
        } ?: run {
            logExtractorFailure("Aksor", playerUrl, "API path was not found in player scripts")
            return null
        }

        val apiUrl = when {
            apiPath.startsWith("http") -> "$apiPath/video/$hash"
            apiPath.startsWith("/") -> "$PLAYER_ORIGIN$apiPath/video/$hash"
            else -> "$PLAYER_ORIGIN/$apiPath/video/$hash"
        }
        val qualities = fetchJson(apiUrl, referer = playerUrl)
            .optJSONObject("qualities")
            ?.toQualityMap()
            ?.takeIf { it.isNotEmpty() }
        return qualities?.values?.lastOrNull()
    }

    private fun extractMetaVideoUrl(html: String): String? =
        Regex("""<meta[^>]+name=["']video_url["'][^>]+content=["']([^"']+)["']""")
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.contains("{{") }

    private suspend fun fetchJson(url: String, referer: String): JSONObject =
        JSONObject(fetchText(url, referer = referer, accept = "application/json"))

    private suspend fun fetchText(url: String, referer: String, accept: String): String =
        httpClient.getText(
            url = url,
            headers = mapOf(
                "Referer" to referer,
                "User-Agent" to CHROME_UA,
                "Accept" to accept,
            ),
        ).body

    private fun AksorResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )
}

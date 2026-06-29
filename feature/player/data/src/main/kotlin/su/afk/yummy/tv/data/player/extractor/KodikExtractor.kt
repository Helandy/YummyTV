package su.afk.yummy.tv.data.player.extractor

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.data.player.utils.BROWSER_STREAM_HEADERS
import su.afk.yummy.tv.data.player.utils.CHROME_UA
import su.afk.yummy.tv.domain.player.isKodikPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import java.net.URLEncoder
import javax.inject.Inject

internal sealed interface KodikResult {
    data class Stream(
        val url: String,
        val qualities: LinkedHashMap<String, String>? = null,
        val headers: Map<String, String> = BROWSER_STREAM_HEADERS,
    ) : KodikResult

    data class Blocked(
        val message: String?,
        val statusCode: Int?,
    ) : KodikResult

    data object Failed : KodikResult
}

internal class KodikExtractor @Inject constructor(
    private val httpClient: PlayerHttpClient,
) : PlayerStreamExtractor {

    private val QUALITY_ORDER = listOf(240, 360, 480, 720, 1080)
    private val HLS_QUALITY_MANIFEST_PATTERN =
        Regex("""/(\d+)\.mp4:hls:manifest\.m3u8(?=$|[?#])""")

    override fun supports(url: String): Boolean = url.isKodikPlayerUrl()

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: android.content.Context,
    ): PlayerStreamResolveResult =
        when (val result = extractStream(request.iframeUrl)) {
            is KodikResult.Stream -> result.toStream()
            is KodikResult.Blocked -> PlayerStreamResolveResult.KodikBlocked(
                message = result.message,
                statusCode = result.statusCode,
            )

            KodikResult.Failed -> PlayerStreamResolveResult.Failed
        }

    private suspend fun extractStream(iframeUrl: String): KodikResult =
        withContext(Dispatchers.IO) {
            val fullUrl = when {
                iframeUrl.startsWith("//") -> "https:$iframeUrl"
                iframeUrl.startsWith("http") -> iframeUrl
                else -> "https://$iframeUrl"
            }
            try {
                // Cookies from the page load are required for /ftor auth
                val (html, cookies) = fetchHtmlWithCookies(fullUrl, referer = "https://yani.tv/")
                val flat = html.replace("\n", "").replace("\r", "")

                val urlParamsStr = Regex("""\burlParams\s*=\s*'([^']+)'""").find(flat)
                    ?.groupValues?.get(1) ?: run {
                    logExtractorFailure("Kodik", fullUrl, "urlParams were not found")
                    return@withContext KodikResult.Failed
                }
                val type = Regex("""\b(?:videoInfo|vInfo)\.type\s*=\s*'([^']+)'""").find(flat)
                    ?.groupValues?.get(1) ?: run {
                    logExtractorFailure("Kodik", fullUrl, "video type was not found")
                    return@withContext KodikResult.Failed
                }
                val hash = Regex("""\b(?:videoInfo|vInfo)\.hash\s*=\s*'([^']+)'""").find(flat)
                    ?.groupValues?.get(1) ?: run {
                    logExtractorFailure("Kodik", fullUrl, "video hash was not found")
                    return@withContext KodikResult.Failed
                }
                val id = Regex("""\b(?:videoInfo|vInfo)\.id\s*=\s*'([^']+)'""").find(flat)
                    ?.groupValues?.get(1) ?: run {
                    logExtractorFailure("Kodik", fullUrl, "video id was not found")
                    return@withContext KodikResult.Failed
                }

                val playerSrc = Regex("""src="((?://[^"]+)?/assets/js/app\.player_single[^"]+)"""")
                    .find(flat)?.groupValues?.get(1) ?: run {
                    logExtractorFailure("Kodik", fullUrl, "player script URL was not found")
                    return@withContext KodikResult.Failed
                }

                // Derive origin from fullUrl so relative paths (/assets/js/...) work too
                val urlOrigin = fullUrl.let { url ->
                    val schemeEnd = url.indexOf("://")
                    if (schemeEnd >= 0) {
                        val afterScheme = url.substring(schemeEnd + 3)
                        val slashIdx = afterScheme.indexOf('/')
                        if (slashIdx >= 0) url.substring(0, schemeEnd + 3 + slashIdx) else url
                    } else url
                }
                val playerScriptUrl = when {
                    playerSrc.startsWith("//") -> "https:$playerSrc"
                    playerSrc.startsWith("/") -> "$urlOrigin$playerSrc"
                    else -> playerSrc
                }
                val origin = playerScriptUrl.substringBefore("/assets/js/")

                val playerScript = fetchHtml(playerScriptUrl, referer = fullUrl)

                // Kodik encodes the endpoint path as base64 inside atob("...") in the player script
                val endpointPath = Regex("""atob\("([A-Za-z0-9+/=]+)"\)""").findAll(playerScript)
                    .mapNotNull { m ->
                        try {
                            val decoded = String(Base64.decode(m.groupValues[1], Base64.DEFAULT))
                            decoded.takeIf { it.startsWith("/") && !decoded.startsWith("//") && decoded.length <= 10 }
                        } catch (_: Exception) {
                            null
                        }
                    }
                    .firstOrNull() ?: "/ftor"

                val endpointUrl = "$origin$endpointPath"

                val urlParams = JSONObject(urlParamsStr)
                val postBody = buildString {
                    append("d=").append(enc(urlParams.optString("d")))
                    append("&d_sign=").append(enc(urlParams.optString("d_sign")))
                    append("&pd=").append(enc(urlParams.optString("pd")))
                    append("&pd_sign=").append(enc(urlParams.optString("pd_sign")))
                    // ref is already URL-encoded in the JSON (e.g. "https%3A%2F%2F..."), use as-is
                    append("&ref=").append(urlParams.optString("ref"))
                    append("&ref_sign=").append(enc(urlParams.optString("ref_sign")))
                    append("&bad_user=true")
                    append("&cdn_is_working=true")
                    append("&type=").append(enc(type))
                    append("&hash=").append(enc(hash))
                    append("&id=").append(enc(id))
                    append("&info=%7B%7D")
                }

                val responseText =
                    postForm(endpointUrl, postBody, referer = fullUrl, cookies = cookies)

                val qualities = parseQualityMap(responseText)
                val streamUrl = qualities?.values?.lastOrNull()
                if (streamUrl != null) {
                    KodikResult.Stream(
                        url = streamUrl,
                        qualities = qualities,
                    )
                } else {
                    logExtractorFailure(
                        "Kodik",
                        endpointUrl,
                        "stream URL was not found in endpoint response"
                    )
                    KodikResult.Failed
                }
            } catch (e: KodikBlockedException) {
                KodikResult.Blocked(
                    message = e.message,
                    statusCode = e.statusCode,
                )
            } catch (e: Exception) {
                logExtractorFailure("Kodik", fullUrl, "unexpected extractor error", e)
                KodikResult.Failed
            }
        }

    private suspend fun parseQualityMap(response: String): LinkedHashMap<String, String>? {
        return try {
            val json = JSONObject(response)
            val links = json.optJSONObject("links") ?: return null
            val qualities = LinkedHashMap<String, String>()
            QUALITY_ORDER.forEach { quality ->
                val src = links.optJSONArray(quality.toString())
                    ?.optJSONObject(0)?.optString("src")
                    ?.takeIf { it.isNotEmpty() } ?: return@forEach
                // src without "//" is ROT18+base64 encoded
                val decoded = if (src.contains("//")) src else decodeKodikSrc(src) ?: return@forEach
                val streamUrl = fixProtocol(decoded)
                val resolved = resolveQualityStreamUrl(streamUrl, quality)
                qualities.putIfAbsent(resolved.label, resolved.url)
            }
            qualities.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private data class QualityStream(
        val label: String,
        val url: String,
    )

    private suspend fun resolveQualityStreamUrl(url: String, expectedQuality: Int): QualityStream {
        val actualQuality =
            hlsManifestQuality(url) ?: return QualityStream("${expectedQuality}p", url)
        if (actualQuality == expectedQuality) return QualityStream("${expectedQuality}p", url)

        if (expectedQuality > actualQuality) {
            val repairedUrl = replaceHlsManifestQuality(url, expectedQuality)
            if (repairedUrl != url && isUrlAvailable(repairedUrl)) {
                return QualityStream("${expectedQuality}p", repairedUrl)
            }
        }

        return QualityStream("${actualQuality}p", url)
    }

    private fun hlsManifestQuality(url: String): Int? =
        HLS_QUALITY_MANIFEST_PATTERN.find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    private fun replaceHlsManifestQuality(url: String, quality: Int): String {
        val match = HLS_QUALITY_MANIFEST_PATTERN.find(url) ?: return url
        return url.replaceRange(match.range, "/$quality.mp4:hls:manifest.m3u8")
    }

    private suspend fun isUrlAvailable(url: String): Boolean =
        runCatching {
            httpClient.head(url = url, headers = mapOf("User-Agent" to CHROME_UA)).isSuccess
        }.getOrDefault(false)

    // Stream URLs from /ftor are encoded: ROT18 applied to letters, then base64 decoded
    private fun decodeKodikSrc(src: String): String? {
        return try {
            val rotated = src.map { c ->
                if (c.isLetter()) {
                    val shifted = c.code + 18
                    val limit = if (c <= 'Z') 90 else 122
                    if (shifted <= limit) shifted.toChar() else (shifted - 26).toChar()
                } else c
            }.joinToString("")
            val padded = rotated + "=".repeat((4 - rotated.length % 4) % 4)
            String(Base64.decode(padded, Base64.DEFAULT))
        } catch (_: Exception) {
            null
        }
    }

    private fun fixProtocol(src: String) = when {
        src.startsWith("//") -> "https:$src"
        src.startsWith("http") -> src
        else -> "https://$src"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private suspend fun fetchHtmlWithCookies(
        url: String,
        referer: String,
    ): Pair<String, String> {
        val response = httpClient.getText(
            url = url,
            headers = mapOf(
                "Referer" to referer,
                "User-Agent" to CHROME_UA,
                "Accept-Language" to "ru-RU,ru;q=0.9,en;q=0.8",
            ),
        )
        if (!response.isSuccess) {
            throw KodikBlockedException(
                message = parseErrorMessage(response.body),
                statusCode = response.statusCode,
            )
        }
        return response.body to response.setCookieHeader
    }

    private fun parseErrorMessage(html: String): String? =
        Regex("""<div[^>]+class=["']message["'][^>]*>([^<]+)<""").find(html)
            ?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    private suspend fun fetchHtml(url: String, referer: String): String =
        httpClient.getText(
            url = url,
            headers = mapOf(
                "Referer" to referer,
                "User-Agent" to CHROME_UA,
                "Accept-Language" to "ru-RU,ru;q=0.9,en;q=0.8",
            ),
        ).body

    private suspend fun postForm(
        url: String,
        body: String,
        referer: String,
        cookies: String
    ): String =
        httpClient.postText(
            url = url,
            body = body,
            headers = buildMap {
                put("Referer", referer)
                put("User-Agent", CHROME_UA)
                put("Content-Type", "application/x-www-form-urlencoded")
                put("X-Requested-With", "XMLHttpRequest")
                if (cookies.isNotEmpty()) put("Cookie", cookies)
            },
        ).body

    private fun KodikResult.Stream.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )
}

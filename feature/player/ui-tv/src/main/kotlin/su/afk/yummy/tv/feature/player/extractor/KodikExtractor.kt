package su.afk.yummy.tv.feature.player.extractor

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import su.afk.yummy.tv.feature.player.view.CHROME_UA
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal sealed interface KodikResult {
    data class Stream(val url: String) : KodikResult
    data class Blocked(val message: String) : KodikResult
    data object Failed : KodikResult
}

internal object KodikExtractor {

    suspend fun extract(
        iframeUrl: String,
        blockedFallback: String,
        serverErrorMessage: (Int) -> String,
    ): KodikResult = withContext(Dispatchers.IO) {
        val fullUrl = when {
            iframeUrl.startsWith("//") -> "https:$iframeUrl"
            iframeUrl.startsWith("http") -> iframeUrl
            else -> "https://$iframeUrl"
        }
        try {
            // Cookies from the page load are required for /ftor auth
            val (html, cookies) = fetchHtmlWithCookies(fullUrl, referer = "https://yani.tv/", serverErrorMessage)
            val flat = html.replace("\n", "").replace("\r", "")

            val urlParamsStr = Regex("""\burlParams\s*=\s*'([^']+)'""").find(flat)
                ?.groupValues?.get(1) ?: run {
                return@withContext KodikResult.Failed
            }
            val type = Regex("""\b(?:videoInfo|vInfo)\.type\s*=\s*'([^']+)'""").find(flat)
                ?.groupValues?.get(1) ?: run {
                return@withContext KodikResult.Failed
            }
            val hash = Regex("""\b(?:videoInfo|vInfo)\.hash\s*=\s*'([^']+)'""").find(flat)
                ?.groupValues?.get(1) ?: run {
                return@withContext KodikResult.Failed
            }
            val id = Regex("""\b(?:videoInfo|vInfo)\.id\s*=\s*'([^']+)'""").find(flat)
                ?.groupValues?.get(1) ?: run {
                return@withContext KodikResult.Failed
            }

            val playerSrc = Regex("""src="((?://[^"]+)?/assets/js/app\.player_single[^"]+)"""")
                .find(flat)?.groupValues?.get(1) ?: run {
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
                    } catch (_: Exception) { null }
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

            val responseText = postForm(endpointUrl, postBody, referer = fullUrl, cookies = cookies)

            val streamUrl = parseStreamUrl(responseText)
            if (streamUrl != null) {
                KodikResult.Stream(streamUrl)
            } else {
                KodikResult.Failed
            }
        } catch (e: KodikBlockedException) {
            KodikResult.Blocked(e.message ?: blockedFallback)
        } catch (_: Exception) {
            KodikResult.Failed
        }
    }

    private fun parseStreamUrl(response: String): String? {
        return try {
            val json = JSONObject(response)
            val links = json.optJSONObject("links") ?: return null
            for (quality in listOf("1080", "720", "480", "360")) {
                val src = links.optJSONArray(quality)
                    ?.optJSONObject(0)?.optString("src")
                    ?.takeIf { it.isNotEmpty() } ?: continue
                // src without "//" is ROT18+base64 encoded
                val decoded = if (src.contains("//")) src else decodeKodikSrc(src) ?: continue
                return fixProtocol(decoded)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

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

    private fun fetchHtmlWithCookies(
        url: String,
        referer: String,
        serverErrorMessage: (Int) -> String,
    ): Pair<String, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Referer", referer)
        conn.setRequestProperty("User-Agent", CHROME_UA)
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
        val code = conn.responseCode
        if (code !in 200..299) {
            val errorHtml = conn.errorStream?.bufferedReader()?.readText() ?: ""
            val message = parseErrorMessage(errorHtml) ?: serverErrorMessage(code)
            throw KodikBlockedException(message)
        }
        val html = conn.inputStream.bufferedReader().readText()
        val cookies = conn.headerFields["Set-Cookie"]
            ?.joinToString("; ") { it.split(";").first() }
            ?: ""
        return html to cookies
    }

    private fun parseErrorMessage(html: String): String? =
        Regex("""<div[^>]+class=["']message["'][^>]*>([^<]+)<""").find(html)
            ?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun fetchHtml(url: String, referer: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Referer", referer)
        conn.setRequestProperty("User-Agent", CHROME_UA)
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
        return conn.inputStream.bufferedReader().readText()
    }

    private fun postForm(url: String, body: String, referer: String, cookies: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 8_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Referer", referer)
        conn.setRequestProperty("User-Agent", CHROME_UA)
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")
        if (cookies.isNotEmpty()) conn.setRequestProperty("Cookie", cookies)
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val bytes = stream?.readBytes() ?: byteArrayOf()
        return bytes.toString(Charsets.UTF_8)
    }
}

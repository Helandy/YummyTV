package su.afk.yummy.tv.feature.player.extractor.common

import java.net.URL

/**
 * Normalizes a URL's scheme only: protocol-relative ("//host/...") and plain-HTTP URLs become
 * HTTPS, an already-HTTPS URL passes through, and anything else (assumed already absolute-ish) is
 * prefixed with "https://". Does not attempt to resolve relative paths - see [resolveRelativeUrl]
 * for that.
 */
fun normalizeUrlScheme(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> url.replaceFirst("http://", "https://")
    url.startsWith("https://") -> url
    else -> "https://$url"
}

fun String.hasKnownUrlScheme(): Boolean =
    startsWith("//") || startsWith("http://") || startsWith("https://")

/**
 * Resolves [raw] as relative to [baseUrl] (RFC 3986 resolution via [URL]). Falls back to
 * [fallback] when [baseUrl] is blank or resolution fails.
 */
fun resolveRelativeUrl(raw: String, baseUrl: String, fallback: () -> String): String =
    if (baseUrl.isBlank()) {
        fallback()
    } else {
        runCatching { URL(URL(baseUrl), raw).toString() }.getOrElse { fallback() }
    }

/** Decodes JSON/JS-style `\uXXXX` unicode escapes embedded in scraped HTML/JSON payloads. */
fun decodeUnicodeEscapes(text: String): String {
    if (!text.contains("\\u")) return text

    val output = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 5 < text.length && text[i + 1] == 'u') {
            val hex = text.substring(i + 2, i + 6)
            runCatching {
                output.append(hex.toInt(16).toChar())
                i += 6
            }.getOrElse {
                output.append(c)
                i++
            }
        } else {
            output.append(c)
            i++
        }
    }
    return output.toString()
        .replace("\\u0026", "&")
        .replace("\\u002D", "-")
        .replace("\\u002d", "-")
        .replace("\\n", "\n")
        .replace("\\\"", "\"")
        .replace("\\'", "'")
        .replace("\\\\", "\\")
}

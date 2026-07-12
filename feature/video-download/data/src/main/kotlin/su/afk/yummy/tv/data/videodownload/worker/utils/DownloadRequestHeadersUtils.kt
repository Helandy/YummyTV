package su.afk.yummy.tv.data.videodownload.worker.utils

import su.afk.yummy.tv.core.utils.httpOriginOrNull
import su.afk.yummy.tv.core.utils.normalizedHttpUrl
import su.afk.yummy.tv.core.utils.safeHttpHeaderNames

internal const val USER_AGENT_HEADER = "User-Agent"

internal fun Map<String, String>.userAgent(): String? =
    entries.firstOrNull { (key, _) -> key.equals(USER_AGENT_HEADER, ignoreCase = true) }?.value

/**
 * @param skipRefererOrigin CVH's CDN (okcdn.ru) rejects requests carrying a foreign Referer/Origin
 * with a 400; its own live playback already works with no Referer/Origin, only User-Agent.
 * @param originOverride used by players (e.g. Alloha) whose CDN expects a fixed Origin that isn't
 * derivable from the iframe URL's own host.
 */
internal fun Map<String, String>.withDownloadRequestHeaders(
    iframeUrl: String,
    skipRefererOrigin: Boolean = false,
    originOverride: String? = null,
): Map<String, String> {
    val hasReferer = keys.any { it.equals(REFERER_HEADER, ignoreCase = true) }
    val hasOrigin = keys.any { it.equals(ORIGIN_HEADER, ignoreCase = true) }
    val hasUserAgent = keys.any { it.equals(USER_AGENT_HEADER, ignoreCase = true) }
    val referer = if (skipRefererOrigin) "" else iframeUrl.normalizedHttpUrl()
    val origin = when {
        skipRefererOrigin -> null
        originOverride != null -> originOverride
        else -> referer.httpOriginOrNull()
    }
    return buildMap {
        putAll(this@withDownloadRequestHeaders)
        if (!hasReferer && referer.isNotBlank()) {
            put(REFERER_HEADER, referer)
        }
        if (!hasOrigin && origin != null) {
            put(ORIGIN_HEADER, origin)
        }
        if (!hasUserAgent) {
            put(USER_AGENT_HEADER, DEFAULT_DOWNLOAD_USER_AGENT)
        }
    }
}

internal fun Map<String, String>.safeHeaderNames(): List<String> =
    safeHttpHeaderNames()

private const val REFERER_HEADER = "Referer"
private const val ORIGIN_HEADER = "Origin"
private const val DEFAULT_DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"

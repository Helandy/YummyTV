package su.afk.yummy.tv.data.videodownload.worker.utils

import su.afk.yummy.tv.core.utils.httpOriginOrNull
import su.afk.yummy.tv.core.utils.normalizedHttpUrl
import su.afk.yummy.tv.core.utils.safeHttpHeaderNames

internal const val USER_AGENT_HEADER = "User-Agent"

internal fun Map<String, String>.userAgent(): String? =
    entries.firstOrNull { (key, _) -> key.equals(USER_AGENT_HEADER, ignoreCase = true) }?.value

internal fun Map<String, String>.withDownloadRequestHeaders(iframeUrl: String): Map<String, String> {
    val referer = iframeUrl.normalizedHttpUrl()
    val origin = if (referer.isAllohaFrameUrl()) ALLOHA_ORIGIN else referer.httpOriginOrNull()
    val hasReferer = keys.any { it.equals(REFERER_HEADER, ignoreCase = true) }
    val hasOrigin = keys.any { it.equals(ORIGIN_HEADER, ignoreCase = true) }
    val hasUserAgent = keys.any { it.equals(USER_AGENT_HEADER, ignoreCase = true) }
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

private fun String.isAllohaFrameUrl(): Boolean =
    contains("alloha", ignoreCase = true)

private const val REFERER_HEADER = "Referer"
private const val ORIGIN_HEADER = "Origin"
private const val ALLOHA_ORIGIN = "https://alloha.yani.tv"
private const val DEFAULT_DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"

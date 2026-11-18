package su.afk.yummy.tv.feature.player.extractor.common

import org.json.JSONObject
import su.afk.yummy.tv.feature.player.extractor.network.PlayerHttpClient

/**
 * GETs [url] with [headers] and returns the response body. Headers vary a lot per balancer
 * (Referer/Origin/Accept/UA combinations), so callers still build their own map - this only
 * removes the repeated response/isSuccess/throw boilerplate around [PlayerHttpClient.getText].
 */
suspend fun PlayerHttpClient.fetchText(
    url: String,
    headers: Map<String, String>,
    throwOnFailure: Boolean = false,
): String {
    val response = getText(url = url, headers = headers)
    if (throwOnFailure && !response.isSuccess) {
        throw IllegalStateException("HTTP ${response.statusCode}: ${response.body.take(80)}")
    }
    return response.body
}

suspend fun PlayerHttpClient.fetchJson(
    url: String,
    headers: Map<String, String>,
    throwOnFailure: Boolean = false,
): JSONObject = JSONObject(fetchText(url = url, headers = headers, throwOnFailure = throwOnFailure))

package su.afk.yummy.tv.feature.player.extractor.network

import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.core.utils.network.HttpHeaderNames

/** Заголовки потока для воспроизведения: только User-Agent выбранного в настройках профиля. */
fun BrowserUserAgentProvider.streamHeaders(): Map<String, String> =
    mapOf(HttpHeaderNames.USER_AGENT to userAgent)

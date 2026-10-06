package su.afk.yummy.tv.data.player.network

import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider

internal const val USER_AGENT_HEADER = "User-Agent"

/** Заголовки потока для воспроизведения: только User-Agent выбранного в настройках профиля. */
internal fun BrowserUserAgentProvider.streamHeaders(): Map<String, String> =
    mapOf(USER_AGENT_HEADER to userAgent)

/** Добавляет [userAgent], если в заголовках его нет или он пустой; существующий не перетирает. */
internal fun Map<String, String>.withBrowserUserAgent(userAgent: String): Map<String, String> {
    val existingUserAgent = entries
        .firstOrNull { it.key.equals(USER_AGENT_HEADER, ignoreCase = true) }
        ?.value
        ?.takeIf { it.isNotBlank() }

    val withoutUserAgent = filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
    return withoutUserAgent + (USER_AGENT_HEADER to (existingUserAgent ?: userAgent))
}

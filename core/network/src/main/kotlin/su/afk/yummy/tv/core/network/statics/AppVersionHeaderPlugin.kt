package su.afk.yummy.tv.core.network.statics

import io.ktor.client.plugins.api.createClientPlugin

const val APP_VERSION_HEADER = "X-App-Version"

class AppVersionHeaderConfig {
    /** Хост своей статики; версия добавляется только к запросам на него. Читается на каждом запросе. */
    var host: () -> String = { "" }

    /** Имя версии установленного приложения; пусто — заголовок не добавляется. */
    var version: String = ""
}

/**
 * Добавляет [APP_VERSION_HEADER] к запросам на собственный хост статики. Чужие хосты (TMDB, yani, Kodik) заголовка не
 * получают: версия приложения им не нужна.
 */
val AppVersionHeaderPlugin = createClientPlugin("AppVersionHeader", ::AppVersionHeaderConfig) {
    val version = pluginConfig.version
    val host = pluginConfig.host
    onRequest { request, _ ->
        val ours = host()
        if (version.isNotBlank() && ours.isNotBlank() && request.url.host.equals(ours, ignoreCase = true)) {
            request.headers.append(APP_VERSION_HEADER, version)
        }
    }
}

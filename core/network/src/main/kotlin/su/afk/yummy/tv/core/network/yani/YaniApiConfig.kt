package su.afk.yummy.tv.core.network.yani

import io.ktor.http.Url
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Адрес API yani, удалённо заменяемый через feature-флаг.
 *
 * Применяется один раз при запуске ([apply]), до создания HTTP-клиента. Пустое, небезопасное
 * (не https) или совпадающее со встроенным [YANI_BASE_URL] значение игнорируется. Подменяются
 * только хост и порт: пути запросов остаются прежними.
 */
@Singleton
class YaniApiConfig @Inject constructor() {

    @Volatile
    var override: YaniApiOverride? = null
        private set

    fun apply(remoteBaseUrl: String) {
        override = parseRemoteBaseUrl(remoteBaseUrl)
    }

    private fun parseRemoteBaseUrl(raw: String): YaniApiOverride? {
        val value = raw.trim().trimEnd('/')
        if (!value.startsWith("https://", ignoreCase = true) || value.any { it.isWhitespace() }) return null
        val url = runCatching { Url(value) }.getOrNull() ?: return null
        if (url.host.isBlank() || url.host.equals(YANI_API_HOST, ignoreCase = true)) return null
        return YaniApiOverride(host = url.host, port = url.port)
    }
}

data class YaniApiOverride(val host: String, val port: Int)

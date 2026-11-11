package su.afk.yummy.tv.source.torrent.data.utils

import java.net.URLEncoder
import java.util.Base64

/** Адрес сервера в виде `scheme://host[:port]` без хвостового `/`; без схемы считаем `http://` (LAN). */
internal fun normalizeBaseUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    if (trimmed.isEmpty()) return trimmed
    return if (trimmed.contains("://")) trimmed else "http://$trimmed"
}

/** Basic-авторизация TorrServer; без логина заголовков нет. */
internal fun basicAuthHeaders(user: String, password: String): Map<String, String> {
    if (user.isBlank()) return emptyMap()
    val token = Base64.getEncoder().encodeToString("$user:$password".toByteArray(Charsets.UTF_8))
    return mapOf("Authorization" to "Basic $token")
}

/** Кодирует сегмент пути/значение запроса: пробел как `%20`, а не `+`. */
internal fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

package su.afk.yummy.tv.core.utils.network

/** Имена HTTP-заголовков, которые приложение ставит само; одно место вместо строк по экстракторам. */
object HttpHeaderNames {
    const val USER_AGENT = "User-Agent"
}

fun Map<String, String>.safeHttpHeaderNames(): List<String> =
    keys.map { it.lowercase() }
        .filterNot { it == "cookie" || it == "authorization" }
        .sorted()

/** Значение `User-Agent` из заголовков (регистр имени не важен); `null`, если его нет или он пустой. */
fun Map<String, String>.userAgentOrNull(): String? =
    entries.firstOrNull { (key, _) -> key.equals(HttpHeaderNames.USER_AGENT, ignoreCase = true) }
        ?.value
        ?.takeIf { it.isNotBlank() }

/**
 * Добавляет [userAgent], если в заголовках его нет или он пустой; существующий не перетирает. Остальные
 * заголовки остаются как есть, имя `User-Agent` приводится к каноническому.
 */
fun Map<String, String>.withUserAgentIfAbsent(userAgent: String): Map<String, String> {
    val existing = userAgentOrNull()
    val withoutUserAgent = filterKeys { !it.equals(HttpHeaderNames.USER_AGENT, ignoreCase = true) }
    return withoutUserAgent + (HttpHeaderNames.USER_AGENT to (existing ?: userAgent))
}

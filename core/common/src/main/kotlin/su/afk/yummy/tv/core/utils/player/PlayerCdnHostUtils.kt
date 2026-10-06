package su.afk.yummy.tv.core.utils.player

import java.net.URL

/**
 * Подписанная ссылка okcdn: подпись от узла не зависит, поэтому один и тот же файл доступен по той
 * же подписи на резервном узле. Это позволяет уехать с отказавшего узла подменой хоста, не
 * перезапрашивая ссылку у балансера.
 */
private const val OK_CDN_HOST_SUFFIX = "okcdn.ru"

private val IPV4_HOST_REGEX = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")

/** Та же ссылка на другом узле CDN. Непригодная для разбора или не-https ссылка возвращается как есть. */
fun String.withCdnHost(host: String): String {
    if (host.isBlank()) return this
    val parsed = runCatching { URL(this) }.getOrNull() ?: return this
    if (!parsed.protocol.equals("https", ignoreCase = true)) return this
    return runCatching {
        URL(parsed.protocol, host, parsed.port, parsed.file).toString()
    }.getOrDefault(this)
}

/** Хост ссылки, или null — разобрать не удалось. */
fun String.cdnHostOrNull(): String? = runCatching { URL(this).host }.getOrNull()

/**
 * Ведёт ли ссылка на okcdn. Проверяется именно хост: в query подписанных ссылок встречается
 * `urls=<адрес>`, поэтому поиск подстроки по всей ссылке давал бы ложные срабатывания.
 */
fun String.isOkCdnUrl(): Boolean = cdnHostOrNull()?.isOkCdnHost() == true

/** Относится ли хост к okcdn. */
fun String.isOkCdnHost(): Boolean = endsWith(OK_CDN_HOST_SUFFIX, ignoreCase = true)

/** Сырой IPv4-адрес вместо имени узла: такие адреса CDN меняет чаще, чем именованные. */
fun String.isIpv4Host(): Boolean = IPV4_HOST_REGEX.matches(this)

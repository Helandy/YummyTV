package su.afk.yummy.tv.source.mikai.data.utils

private const val ASHDI_PREFIX = "source://mikai/ashdi/"
private const val FENIX_PREFIX = "source://mikai/fenix/"

/** Id записи в плеере Ashdi (`https://ashdi.vip/vod/<id>`). */
private val VOD_ID = Regex("^\\d{1,12}$")
private val VOD_LINK = Regex("""^https://ashdi\.vip/vod/(\d{1,12})(?:[?#].*)?$""")

/** Токен серии Fenix (`https://fenixplay.xyz/vod/<токен>`): подписанный путь, в нём серия и озвучка. */
private val FENIX_TOKEN = Regex("^[A-Za-z0-9_.-]{1,2048}$")
private val FENIX_LINK = Regex("""^https://fenixplay\.xyz/vod/([A-Za-z0-9_.-]{1,2048})(?:[?#].*)?$""")

internal fun isMikaiSourceUrl(url: String): Boolean =
    url.startsWith(ASHDI_PREFIX, ignoreCase = true) || url.startsWith(FENIX_PREFIX, ignoreCase = true)

/** `source://mikai/ashdi/{vodId}`; провайдеры Mikai, кроме Ashdi и Fenix, приложением пока не играются. */
internal fun mikaiSourceUrl(vodId: String): String = "$ASHDI_PREFIX$vodId"

/** Id записи из `source://mikai/ashdi/{vodId}`; `null` для чужих и испорченных адресов. */
internal fun parseMikaiSourceUrl(url: String): String? {
    if (!url.startsWith(ASHDI_PREFIX, ignoreCase = true)) return null
    return url.substring(ASHDI_PREFIX.length).takeIf { VOD_ID.matches(it) }
}

/** Id записи серии: поле `id` ответа сервера, а если его нет — из адреса страницы плеера. */
internal fun mikaiVodId(id: String?, url: String?): String? =
    id?.takeIf { VOD_ID.matches(it) } ?: url?.let { VOD_LINK.find(it)?.groupValues?.get(1) }

/** Адрес страницы плеера Ashdi для записи [vodId]. */
internal fun mikaiAshdiPageUrl(vodId: String): String = "https://ashdi.vip/vod/$vodId"

/** `source://mikai/fenix/{токен}`. */
internal fun mikaiFenixSourceUrl(token: String): String = "$FENIX_PREFIX$token"

/** Токен из `source://mikai/fenix/{токен}`; `null` для чужих и испорченных адресов. */
internal fun parseMikaiFenixSourceUrl(url: String): String? {
    if (!url.startsWith(FENIX_PREFIX, ignoreCase = true)) return null
    return url.substring(FENIX_PREFIX.length).takeIf { FENIX_TOKEN.matches(it) }
}

/** Токен серии Fenix: поле `id` ответа сервера, а если его нет — из адреса страницы плеера (фрагмент `#embed=…` отбрасывается). */
internal fun mikaiFenixToken(id: String?, url: String?): String? =
    id?.takeIf { FENIX_TOKEN.matches(it) } ?: url?.let { FENIX_LINK.find(it)?.groupValues?.get(1) }

/** Адрес страницы плеера Fenix для токена [token]. */
internal fun mikaiFenixPageUrl(token: String): String = "https://fenixplay.xyz/vod/$token"

package su.afk.yummy.tv.source.mikai.data.utils

private const val ASHDI_PREFIX = "source://mikai/ashdi/"
private const val FENIX_PREFIX = "source://mikai/fenix/"
private const val MOON_PREFIX = "source://mikai/moon/"

/** Id записи в плеере Ashdi (`https://ashdi.vip/vod/<id>`). */
private val VOD_ID = Regex("^\\d{1,12}$")
private val VOD_LINK = Regex("""^https://ashdi\.vip/vod/(\d{1,12})(?:[?#].*)?$""")

/** Токен серии Fenix (`https://fenixplay.xyz/vod/<токен>`): подписанный путь, в нём серия и озвучка. */
private val FENIX_TOKEN = Regex("^[A-Za-z0-9_.-]{1,2048}$")
private val FENIX_LINK = Regex("""^https://fenixplay\.xyz/vod/([A-Za-z0-9_.-]{1,2048})(?:[?#].*)?$""")

/** Id страницы плеера Moon (`https://moonanime.art/iframe/<id>/`). */
private val MOON_ID = Regex("^[A-Za-z0-9]{4,64}$")
private val MOON_LINK = Regex("""^https://moonanime\.art/iframe/([A-Za-z0-9]{4,64})/?(?:[?#].*)?$""")

internal fun isMikaiSourceUrl(url: String): Boolean =
    url.startsWith(ASHDI_PREFIX, ignoreCase = true) ||
        url.startsWith(FENIX_PREFIX, ignoreCase = true) ||
        url.startsWith(MOON_PREFIX, ignoreCase = true)

/** `source://mikai/ashdi/{vodId}`; у Fenix и Moon свои схемы (`source://mikai/fenix/…`, `source://mikai/moon/…`). */
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

/** `source://mikai/moon/{id}`. */
internal fun mikaiMoonSourceUrl(id: String): String = "$MOON_PREFIX$id"

/** Id из `source://mikai/moon/{id}`; `null` для чужих и испорченных адресов. */
internal fun parseMikaiMoonSourceUrl(url: String): String? {
    if (!url.startsWith(MOON_PREFIX, ignoreCase = true)) return null
    return url.substring(MOON_PREFIX.length).takeIf { MOON_ID.matches(it) }
}

/** Id страницы плеера Moon из адреса `embedUrl`. */
internal fun mikaiMoonId(url: String?): String? = url?.let { MOON_LINK.find(it)?.groupValues?.get(1) }

/** Адрес страницы плеера Moon для [id]. */
internal fun mikaiMoonPageUrl(id: String): String = "https://moonanime.art/iframe/$id/"

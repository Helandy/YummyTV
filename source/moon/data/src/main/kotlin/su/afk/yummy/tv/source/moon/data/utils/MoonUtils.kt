package su.afk.yummy.tv.source.moon.data.utils

import kotlin.io.encoding.Base64

private val PAYLOAD = Regex("""atob\("([A-Za-z0-9+/=]+)"\)""")
private val KEY = Regex("""var k\s*=\s*"([^"]+)"""")
private val FILE = Regex("""file:\s*\w+\("([A-Za-z0-9+/=]+)"\)""")
private val HLS = Regex("""https?://[^"'\s,\\\]]+?\.m3u8[^"'\s,\\\]]*""")
private val LABELED = Regex("""\[([^\]]*)](https?://[^,\s]+)""")
private val DIGITS = Regex("""\d+""")

/** Размер заголовка payload: байт начального состояния и 32 байта ключа потока. */
private const val HEADER_SIZE = 33
private const val KEY_SIZE = 32
private const val MAX_QUALITY = 1080

/** Результат разбора плеера Moon: основной адрес и, если страница отдаёт список, карта качеств по возрастанию. */
internal data class MoonManifest(val url: String, val qualities: LinkedHashMap<String, String>? = null)

/** Скрипт плеера из `atob("…")` страницы Moon; `null`, если обёртка не найдена или не декодируется. */
internal fun moonScript(page: String): String? =
    PAYLOAD.find(page)?.groupValues?.get(1)?.let(::decodeMoonPayload)

/** Payload: байт 0 — начальное состояние, байты 1..32 — ключ, дальше шифртекст; состояние обновляется суммой байтов. */
internal fun decodeMoonPayload(base64: String): String? {
    val bytes = runCatching { Base64.Default.decode(base64) }.getOrNull() ?: return null
    if (bytes.size <= HEADER_SIZE) return null
    var state = bytes[0].toInt() and 0xFF
    val out = ByteArray(bytes.size - HEADER_SIZE)
    for (i in out.indices) {
        val cipher = bytes[HEADER_SIZE + i].toInt() and 0xFF
        val key = bytes[1 + i % KEY_SIZE].toInt() and 0xFF
        out[i] = (state xor cipher xor key).toByte()
        state = (cipher + key) and 0xFF
    }
    return out.decodeToString()
}

/** Манифест из расшифрованного скрипта: `var k="…"` — ключ, `file: f("…")` — base64 строки, зашифрованной этим ключом. */
internal fun moonManifest(script: String): MoonManifest? {
    val key = KEY.find(script)?.groupValues?.get(1)?.encodeToByteArray()?.takeIf { it.isNotEmpty() } ?: return null
    val raw = FILE.find(script)?.groupValues?.get(1)
        ?.let { runCatching { Base64.Default.decode(it) }.getOrNull() } ?: return null
    val text = ByteArray(raw.size) { (raw[it].toInt() xor key[it % key.size].toInt()).toByte() }.decodeToString()
    return parseMoonFile(text)
}

internal fun parseMoonFile(text: String): MoonManifest? {
    HLS.find(text)?.let { return MoonManifest(it.value) }
    val labeled = LABELED.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()
    if (labeled.isNotEmpty()) {
        val ascending = labeled.sortedBy { moonQuality(it.first) }
        val best = ascending.lastOrNull { moonQuality(it.first) <= MAX_QUALITY } ?: ascending.first()
        return MoonManifest(best.second, LinkedHashMap(ascending.associate { it.first to it.second }))
    }
    val single = text.trim()
    return MoonManifest(single).takeIf { single.startsWith("http") && single.none { it.isWhitespace() || it == ',' } }
}

private fun moonQuality(label: String): Int = DIGITS.find(label)?.value?.toIntOrNull() ?: 0

/** Зеркала сайта Moon: AnimeON отдаёт страницы плеера то с `.art`, то с `.net` (у плеера они перечислены как свои хосты). */
private val PAGE = Regex("""^(https://moonanime\.(?:art|net|vip))/iframe/([A-Za-z0-9]{4,64})/?(?:[?#].*)?$""")

/** Страница плеера Moon: [origin] — `https://moonanime.art` (или зеркало), [id] — id страницы. */
internal data class MoonPage(val origin: String, val id: String) {
    /** Адрес страницы без параметров: страница с `?player=…` отдаёт то же, а чистый адрес проверен. */
    val url: String get() = "$origin/iframe/$id/"
}

/** Разбор адреса `https://moonanime.{art|net|vip}/iframe/{id}/…`; `null` для чужих адресов. */
internal fun moonPage(url: String): MoonPage? =
    PAGE.find(url)?.let { MoonPage(origin = it.groupValues[1], id = it.groupValues[2]) }

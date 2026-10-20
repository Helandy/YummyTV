package su.afk.yummy.tv.source.aniboom.data.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val DATA_PARAMETERS = Regex("data-parameters=\"([^\"]*)\"")
private val parser = Json { ignoreUnknownKeys = true }

/** Адрес эмбеда серии; `parent` не нужен, а `Referer: https://animego.me/` обязателен (без него 404). */
internal fun aniboomEmbedUrl(aniboomId: String, episode: Int, translation: Int): String =
    "https://aniboom.one/embed/$aniboomId?episode=$episode&translation=$translation&episodes=1"

internal const val ANIBOOM_REFERER = "https://animego.me/"

/**
 * Мастер-плейлист HLS из страницы эмбеда: `<video data-parameters="{…,&quot;hls&quot;:&quot;{\"src\":…}&quot;}">`.
 * Поле `hls` — JSON, вложенный строкой. `null`, если разметка не та или ссылки нет.
 */
internal fun parseAniboomHlsUrl(html: String): String? {
    val raw = DATA_PARAMETERS.find(html)?.groupValues?.get(1) ?: return null
    return runCatching {
        val parameters = parser.parseToJsonElement(unescapeHtml(raw)).jsonObject
        val hls = parameters["hls"]?.jsonPrimitive?.contentOrNull ?: return null
        val src = (parser.parseToJsonElement(hls) as? JsonObject)?.get("src")?.jsonPrimitive?.contentOrNull
        src?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }.getOrNull()
}

private fun unescapeHtml(value: String): String = value
    .replace("&quot;", "\"").replace("&#039;", "'").replace("&#x27;", "'")
    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

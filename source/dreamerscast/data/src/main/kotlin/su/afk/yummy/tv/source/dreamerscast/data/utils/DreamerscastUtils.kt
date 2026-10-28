package su.afk.yummy.tv.source.dreamerscast.data.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

internal const val DREAMERSCAST_BASE_URL = "https://dreamerscast.com"

private val json = Json { ignoreUnknownKeys = true }

private val PLAYERJS = Regex("""new Playerjs\("(#2[A-Za-z0-9+/=]+)"\)""")
private val FIRST_NUMBER = Regex("""\d{1,4}""")
private val RESOLUTION = Regex("RESOLUTION=\\d+x(\\d+)", RegexOption.IGNORE_CASE)

/** Длина вставки-«мусора» в строке Playerjs: `//` + 86 символов base64 + `==`. */
private const val JUNK_LENGTH = 90

@Serializable
private data class PlayerPlaylist(val file: List<PlayerEpisode> = emptyList())

@Serializable
private data class PlayerEpisode(val title: String = "", val file: String = "")

/** Адрес страницы релиза. */
internal fun dreamerscastPageUrl(slug: String): String = "$DREAMERSCAST_BASE_URL/home/release/$slug"

/**
 * Плейлист серий из `new Playerjs("#2…")` на странице релиза. Строка — base64 JSON `{"file":[{"title":"Серия 1","file":"…"},…]}`
 * со вставками-«мусором»: `//` + 86 символов + `==`. Вставки бывают вложенными (позже вставленная легла внутрь прежней), поэтому
 * снимаются по очереди от первого `==` назад на 88 символов, пока не останется только финальный паддинг строки. Любое
 * отклонение от формата даёт `null`: показывать лучше «серии нет», чем падать.
 */
internal fun dreamerscastPlaylist(html: String): List<Pair<String, String>>? {
    val encoded = PLAYERJS.find(html)?.groupValues?.get(1)?.substring(2) ?: return null
    val sb = StringBuilder(encoded)
    while (true) {
        val end = sb.indexOf("==")
        if (end < 0 || end + 2 == sb.length) break
        val start = end - (JUNK_LENGTH - 2)
        if (start < 0 || sb[start] != '/' || sb[start + 1] != '/') return null
        sb.delete(start, end + 2)
    }
    val raw = sb.toString().trimEnd('=')
    val text = runCatching { Base64.Default.decode(raw.padEnd((raw.length + 3) / 4 * 4, '=')).decodeToString() }.getOrNull() ?: return null
    val playlist = runCatching { json.decodeFromString<PlayerPlaylist>(text) }.getOrNull() ?: return null
    return playlist.file.map { it.title to it.file }
}

/**
 * Файл серии [episode]: по числу в названии («Серия 5»), а если числа нет (фильм, OVA) — по порядку. В поле `file` лежит
 * `«…/manifest.mpd or …/master.m3u8»`: берётся HLS-мастер. `null`, если серии нет или HLS-ссылки в ней нет.
 */
internal fun dreamerscastHls(playlist: List<Pair<String, String>>, episode: Int): String? {
    val entry = playlist.firstOrNull { (title, _) -> FIRST_NUMBER.find(title)?.value?.toIntOrNull() == episode }
        ?: playlist.getOrNull(episode - 1)?.takeIf { (title, _) -> FIRST_NUMBER.find(title) == null }
        ?: return null
    return entry.second.split(" or ").map(String::trim)
        .firstOrNull { it.startsWith("https://play.dreamerscast.com/") && it.substringBefore('?').endsWith(".m3u8") }
}

/** Качества из мастера по возрастанию высоты, "720p" → адрес варианта; звуковые дорожки без `RESOLUTION` пропускаются. */
internal fun dreamerscastQualities(master: String): LinkedHashMap<String, String> {
    val lines = master.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (lines.firstOrNull()?.startsWith("#EXTM3U") != true) return LinkedHashMap()
    val variants = lines.withIndex().mapNotNull { (i, line) ->
        if (!line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) return@mapNotNull null
        val height = RESOLUTION.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
        val url = lines.getOrNull(i + 1)?.takeIf { it.startsWith("https://") } ?: return@mapNotNull null
        height to url
    }
    val result = LinkedHashMap<String, String>()
    variants.sortedBy { it.first }.forEach { (height, url) -> result["${height}p"] = url }
    return result
}

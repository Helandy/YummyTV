package su.afk.yummy.tv.source.animebesst.data.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

private val json = Json { ignoreUnknownKeys = true }

private val VIDEO_LIST = Regex("""var videoList\s*=\s*(\[.*?])\s*;""", RegexOption.DOT_MATCHES_ALL)

/** Метка серии: `5`, `1-2` (сдвоенная серия), после номера может идти название озвучки. */
private val EPISODE_LABEL = Regex("""^\s*(\d{1,4})(?:\s*-\s*(\d{1,4}))?""")
private val PLAYER_URL = Regex("""^https://[a-z0-9.-]{3,100}/vod/\d{1,12}$""")
private val FILE = Regex("""\bfile\s*:\s*["'](https://[^"'\s]+\.m3u8[^"'\s]*)["']""")
private val RESOLUTION = Regex("RESOLUTION=\\d+x(\\d+)", RegexOption.IGNORE_CASE)

@Serializable
private data class VideoEntry(val id: String = "", val link: String = "")

/** Адрес страницы тайтла на хосте [host]. */
internal fun animebesstPageUrl(host: String, slug: String): String = "https://$host/anime1-online/$slug.html"

/**
 * Страница плеера серии [episode] из `var videoList = [{"id":"1","link":"//host/vod/96032"},…]`; `null`, если списка или серии
 * нет, либо ссылка ведёт не на `/vod/{id}`. Метка `1-2` покрывает обе серии.
 */
internal fun animebesstPlayerUrl(html: String, episode: Int): String? {
    val list = VIDEO_LIST.find(html)?.groupValues?.get(1) ?: return null
    val entries = runCatching { json.decodeFromString<List<VideoEntry>>(list) }.getOrNull() ?: return null
    val link = entries.firstOrNull { entry ->
        val label = EPISODE_LABEL.find(entry.id) ?: return@firstOrNull false
        val from = label.groupValues[1].toInt()
        val to = label.groupValues[2].toIntOrNull() ?: from
        episode in from..to
    }?.link ?: return null
    val url = if (link.startsWith("//")) "https:$link" else link
    return url.takeIf { PLAYER_URL.matches(it) }
}

/** HLS-мастер из `new Playerjs({ file:"…/index.m3u8" })` на странице плеера. */
internal fun animebesstMaster(html: String): String? = FILE.find(html)?.groupValues?.get(1)

/**
 * Качества из мастера по возрастанию высоты, "720p" → адрес варианта. Адреса вариантов у плеера относительные
 * (`./1080/index.m3u8`) и считаются от адреса мастера.
 */
internal fun animebesstQualities(master: String, masterUrl: String): LinkedHashMap<String, String> {
    val lines = master.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (lines.firstOrNull()?.startsWith("#EXTM3U") != true) return LinkedHashMap()
    val variants = lines.withIndex().mapNotNull { (i, line) ->
        if (!line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) return@mapNotNull null
        val height = RESOLUTION.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
        val ref = lines.getOrNull(i + 1)?.takeIf { !it.startsWith("#") } ?: return@mapNotNull null
        val url = runCatching { URI(masterUrl).resolve(ref).toString() }.getOrNull()?.takeIf { it.startsWith("https://") }
            ?: return@mapNotNull null
        height to url
    }
    val result = LinkedHashMap<String, String>()
    variants.sortedBy { it.first }.forEach { (height, url) -> result["${height}p"] = url }
    return result
}

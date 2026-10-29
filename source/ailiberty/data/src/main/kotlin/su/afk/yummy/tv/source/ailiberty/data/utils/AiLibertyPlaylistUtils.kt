package su.afk.yummy.tv.source.ailiberty.data.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal const val AILIBERTY_BASE_URL = "https://ailiberty.top"

/** Скрипт плеера Playerjs: `file: [...]`, дальше `default_quality` (запасной вариант — до закрывающей скобки объекта). */
private val PLAYLIST = Regex("""file\s*:\s*(\[.*?])\s*,?\s*default_quality""", RegexOption.DOT_MATCHES_ALL)
private val PLAYLIST_FALLBACK = Regex("""file\s*:\s*(\[.*?])\s*\}""", RegexOption.DOT_MATCHES_ALL)

/** `[480p]/video/….m3u8?expires=…,[720p]/video/….m3u8` — у каждого качества свой адрес. */
private val QUALITY_LINK = Regex("""\[(\d{3,4})p]([^\[,]+)""")
private val EPISODE_TITLE = Regex("""(\d{1,4})""")

private val json = Json { ignoreUnknownKeys = true }

private class PlaylistItem(val title: String, val file: String)

/** Адрес страницы релиза на сайте. */
internal fun aiLibertyReleaseUrl(slug: String): String = "$AILIBERTY_BASE_URL/releases/$slug"

/**
 * Качества серии [episode] со страницы релиза: качество → абсолютный адрес, по возрастанию качества. Серия ищется по числу в
 * названии («Эпизод 3»), без совпадения — по порядку в плейлисте. `null`, если в странице нет плейлиста или такой серии.
 */
internal fun aiLibertyEpisodeQualities(html: String, episode: Int): Map<String, String>? {
    val items = playlistItems(html)
    val item = items.firstOrNull { EPISODE_TITLE.find(it.title)?.groupValues?.get(1)?.toIntOrNull() == episode }
        ?: items.getOrNull(episode - 1)
        ?: return null
    val links = QUALITY_LINK.findAll(item.file)
        .map { it.groupValues[1].toInt() to absolute(it.groupValues[2].trim()) }
        .sortedBy { it.first }
        .toList()
    if (links.isEmpty()) return null
    return LinkedHashMap<String, String>().apply { links.forEach { (height, url) -> put("${height}p", url) } }
}

private fun playlistItems(html: String): List<PlaylistItem> {
    val array = (PLAYLIST.find(html) ?: PLAYLIST_FALLBACK.find(html))?.groupValues?.get(1) ?: return emptyList()
    val parsed = runCatching { json.parseToJsonElement(array) }.getOrNull() as? JsonArray ?: return emptyList()
    return parsed.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val file = obj["file"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        PlaylistItem(title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(), file = file)
    }
}

private fun absolute(link: String): String = when {
    link.startsWith("http://") || link.startsWith("https://") -> link
    link.startsWith("/") -> AILIBERTY_BASE_URL + link
    else -> "$AILIBERTY_BASE_URL/$link"
}

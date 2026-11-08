// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.source.sameband.data.utils

import kotlinx.serialization.json.Json
import su.afk.yummy.tv.source.sameband.data.dto.SamebandPlaylistItemDto

internal const val SAMEBAND_BASE_URL = "https://sameband.studio"

private val IFRAME_PLAYER = Regex("""<iframe[^>]+src\s*=\s*["']([^"']*/v/play/[^"']+)["']""", RegexOption.IGNORE_CASE)
private val PLAYLIST_FILE = Regex("""file\s*:\s*["']([^"']+_list\.txt[^"']*)["']""")

// Путь может содержать пробелы («/v/anime/Grand Blue Season 3/…»): запись кончается запятой перед следующим «[» или концом строки.
private val QUALITY_ENTRY = Regex("""\[(\d{3,4})p?]\s*(.+?)(?=\s*,\s*\[\d{3,4}p?]|\s*$)""")
private val EPISODE_AFTER_WORD = Regex("""сери[яи]\s*(\d+)""", RegexOption.IGNORE_CASE)
private val EPISODE_BEFORE_WORD = Regex("""(\d+)\s*сери""", RegexOption.IGNORE_CASE)
private val SLUG_IN_URL = Regex("""/anime/(\d+-[A-Za-z0-9_-]+)\.html""")
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/** Адрес страницы тайтла на сайте. */
internal fun samebandPageUrl(slug: String): String = "$SAMEBAND_BASE_URL/anime/$slug.html"

/** Slug (`1234-grand-blue`) из адреса страницы; `null`, если адрес не похож на страницу тайтла. */
internal fun samebandSlugFromUrl(url: String): String? = SLUG_IN_URL.find(url)?.groupValues?.get(1)

/** Относительный путь сайта (`/v/...`) в абсолютный адрес; абсолютные адреса остаются как есть. */
internal fun samebandAbsoluteUrl(path: String): String = when {
    path.startsWith("http://") || path.startsWith("https://") -> path
    path.startsWith("//") -> "https:$path"
    path.startsWith("/") -> SAMEBAND_BASE_URL + path
    else -> "$SAMEBAND_BASE_URL/$path"
}.replace(" ", "%20")

/** Адрес страницы плеера из iframe на странице тайтла; `null`, если плеера нет. */
internal fun samebandPlayerUrl(pageHtml: String): String? =
    IFRAME_PLAYER.find(pageHtml)?.groupValues?.get(1)?.let(::samebandAbsoluteUrl)

/** Адрес JSON-плейлиста из Playerjs (`file: "/v/list/X_list.txt"`) на странице плеера. */
internal fun samebandPlaylistUrl(playerHtml: String): String? =
    PLAYLIST_FILE.find(playerHtml)?.groupValues?.get(1)?.let(::samebandAbsoluteUrl)

/** Разбор JSON-плейлиста; пусто, если это не массив элементов. */
internal fun parseSamebandPlaylist(text: String): List<SamebandPlaylistItemDto> =
    runCatching { json.decodeFromString<List<SamebandPlaylistItemDto>>(text.trim().removePrefix("")) }
        .getOrDefault(emptyList())

/** Номер серии из HTML-подписи («Серия 03» / «3 серия»); при отсутствии — [fallback]. */
internal fun samebandEpisodeNumber(title: String?, fallback: Int): Int {
    val text = title.orEmpty().replace(Regex("<[^>]*>"), " ")
    // «Серия 01» проверяется первой: «24:10 Серия 01» иначе даст 10 из длительности.
    val match = EPISODE_AFTER_WORD.find(text) ?: EPISODE_BEFORE_WORD.find(text) ?: return fallback
    return match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: fallback
}

/** Качества из `[480p]a,[720p]b,[1080p]c` → «480p» → абсолютный адрес, по возрастанию высоты. */
internal fun samebandQualities(file: String): Map<String, String> =
    QUALITY_ENTRY.findAll(file)
        .map { it.groupValues[1].toInt() to samebandAbsoluteUrl(it.groupValues[2]) }
        .toMap()
        .toSortedMap()
        .entries.associateTo(LinkedHashMap()) { "${it.key}p" to it.value }

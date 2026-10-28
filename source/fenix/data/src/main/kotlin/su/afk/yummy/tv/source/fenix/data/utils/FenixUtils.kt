package su.afk.yummy.tv.source.fenix.data.utils

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.source.fenix.data.dto.FenixAccessDto
import su.afk.yummy.tv.source.fenix.data.dto.FenixPlaylistDto
import su.afk.yummy.tv.source.fenix.data.model.FenixPage

private const val DEFAULT_EDITION = "tv"
private const val FENIX_ORIGIN = "https://fenixplay.xyz"

private val PAGE_URL = Regex("""^https://fenixplay\.xyz(/(vod|embed)/[A-Za-z0-9_.-]{1,2048})(?:\?([^#\s]*))?(?:#.*)?$""")
private val MEDIA_URL = Regex("""^https://(?:[a-z0-9-]+\.)*fenixplay\.xyz/""")
private val PLAYLIST_MARKER = Regex("""FENIX_PLAYLIST\s*=\s*(?=\{)""")
private val ACCESS_MARKER = Regex("""FENIX_MEDIA_ACCESS\s*=\s*(?=\{)""")
private val json = Json { ignoreUnknownKeys = true }

/** Страница плеера Fenix (`/vod/{токен}` или `/embed/{токен}?ep=…&edition=…`); другие хосты и пути не принимаются. */
internal fun parseFenixPage(url: String): FenixPage? {
    val match = PAGE_URL.find(url) ?: return null
    val query = match.groupValues[3].split('&').mapNotNull { part ->
        part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
    }.toMap()
    return FenixPage(
        path = match.groupValues[1],
        isVod = match.groupValues[2] == "vod",
        episode = query["ep"]?.toIntOrNull()?.takeIf { it > 0 },
        edition = query["edition"]?.lowercase()?.takeIf { it.isNotEmpty() },
    )
}

/** Адрес страницы `/vod/` для токена серии из ответа сервера; `null` для испорченного токена. */
internal fun fenixVodPageUrl(token: String): String? = "$FENIX_ORIGIN/vod/$token".takeIf { PAGE_URL.matches(it) }

/** Список серий из `window.FENIX_PLAYLIST = {…}`; `null`, если страница устроена иначе. */
internal fun fenixPlaylist(html: String): FenixPlaylistDto? =
    jsonObjectAfter(html, PLAYLIST_MARKER)?.let { decode<FenixPlaylistDto>(it) }

/** Токен доступа к медиа из `window.FENIX_MEDIA_ACCESS = {…}`. */
internal fun fenixAccessToken(html: String): String? =
    jsonObjectAfter(html, ACCESS_MARKER)?.let { decode<FenixAccessDto>(it) }?.token?.takeIf { it.isNotBlank() }

/**
 * HLS-мастер выбранной озвучки серии. На `/vod/` серия одна, озвучка — та, чей `vod_url` равен адресу страницы. На `/embed/`
 * серия — по `ep` и `edition`, озвучка — по названию [track] без учёта регистра. Без совпадения играет дорожка по умолчанию
 * (оригинал). `null`, если серии нет.
 */
internal fun fenixMasterUrl(playlist: FenixPlaylistDto, page: FenixPage, track: String?): String? {
    val episode = if (page.isVod) {
        playlist.episodes.firstOrNull()
    } else {
        val number = page.episode ?: return null
        val edition = page.edition ?: playlist.defaultEdition?.lowercase() ?: DEFAULT_EDITION
        playlist.episodes.firstOrNull { it.number == number && (it.edition ?: DEFAULT_EDITION).lowercase() == edition }
    } ?: return null
    val chosen = episode.tracks.firstOrNull { page.isVod && it.vodUrl == page.path }
        ?: track?.let { wanted -> episode.tracks.firstOrNull { it.label.equals(wanted, ignoreCase = true) } }
        ?: episode.tracks.firstOrNull { it.isDefault }
        ?: episode.tracks.firstOrNull()
    return chosen?.hlsUrl ?: episode.hlsMasterUrl
}

/** Мастер с токеном доступа в query. Токен уходит только на хосты Fenix: чужой адрес в плейлисте даёт `null`. */
internal fun signedFenixMaster(master: String, token: String): String? {
    if (!MEDIA_URL.containsMatchIn(master)) return null
    if (master.contains("token=")) return master
    return master + (if ('?' in master) "&" else "?") + "token=" + token
}

private inline fun <reified T> decode(source: String): T? =
    try {
        json.decodeFromString<T>(source)
    } catch (_: SerializationException) {
        null
    }

/** Объект `{…}`, начинающийся сразу после первого совпадения [marker]: скобки считаются с учётом строк и экранирования. */
private fun jsonObjectAfter(html: String, marker: Regex): String? {
    val start = marker.find(html)?.range?.last?.plus(1) ?: return null
    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until html.length) {
        val c = html[i]
        when {
            escaped -> escaped = false
            inString -> when (c) {
                '\\' -> escaped = true
                '"' -> inString = false
            }
            c == '"' -> inString = true
            c == '{' -> depth++
            c == '}' -> if (--depth == 0) return html.substring(start, i + 1)
        }
    }
    return null
}

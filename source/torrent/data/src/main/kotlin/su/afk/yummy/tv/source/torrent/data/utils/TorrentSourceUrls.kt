package su.afk.yummy.tv.source.torrent.data.utils

import su.afk.yummy.tv.source.torrent.data.source.TorrentTarget
import java.net.URLDecoder

private const val PREFIX = "source://torrent/"

internal fun isTorrentSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://torrent/{animeId}/{episode}?q={запрос}&q={запрос}`: серия и запросы закодированы. */
internal fun TorrentTarget.toSourceUrl(): String =
    PREFIX + animeId + "/" + episode.urlEncoded() +
        queries.joinToString(separator = "&", prefix = "?") { "q=" + it.urlEncoded() }

internal fun parseTorrentSourceUrl(url: String): TorrentTarget? {
    if (!isTorrentSourceUrl(url)) return null
    val body = url.substring(PREFIX.length)
    val path = body.substringBefore('?')
    val query = body.substringAfter('?', "")
    val parts = path.split('/')
    if (parts.size != 2) return null
    val animeId = parts[0].toIntOrNull() ?: return null
    val queries = query.split('&')
        .filter { it.startsWith("q=") }
        .map { URLDecoder.decode(it.removePrefix("q="), "UTF-8") }
        .filter { it.isNotBlank() }
    if (queries.isEmpty()) return null
    return TorrentTarget(animeId = animeId, episode = URLDecoder.decode(parts[1], "UTF-8"), queries = queries)
}

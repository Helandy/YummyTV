package su.afk.yummy.tv.source.anime4k.data.utils

import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import java.net.URLDecoder
import java.net.URLEncoder

private const val PREFIX = "source://anime4k/"

internal fun isAnime4kSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://anime4k/{malId}/{season}/{episode}/{team}`: сезон, серия и озвучка закодированы. */
internal fun Anime4kTarget.toSourceUrl(): String =
    PREFIX + listOf(malId.toString(), season.encode(), episode.encode(), team.encode()).joinToString("/")

internal fun parseAnime4kSourceUrl(url: String): Anime4kTarget? {
    if (!isAnime4kSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 4) return null
    val malId = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
    val team = parts[3].decode().takeIf { it.isNotBlank() } ?: return null
    return Anime4kTarget(malId = malId, season = parts[1].decode(), episode = parts[2].decode(), team = team)
}

private fun String.encode(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

private fun String.decode(): String = URLDecoder.decode(this, "UTF-8")

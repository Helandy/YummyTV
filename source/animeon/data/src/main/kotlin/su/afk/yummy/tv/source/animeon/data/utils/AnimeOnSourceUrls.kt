package su.afk.yummy.tv.source.animeon.data.utils

import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget

private const val PREFIX = "source://animeon/"

private val TITLE_KEY = Regex("^\\d{1,9}$")

internal fun isAnimeOnSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://animeon/{title}/{episode}/{translation}/{player}`. */
internal fun AnimeOnTarget.toSourceUrl(): String = "$PREFIX$titleKey/$episode/$translationId/$playerId"

internal fun parseAnimeOnSourceUrl(url: String): AnimeOnTarget? {
    if (!isAnimeOnSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 4) return null
    val title = parts[0].takeIf { TITLE_KEY.matches(it) } ?: return null
    val numbers = parts.drop(1).map { it.toIntOrNull()?.takeIf { n -> n > 0 } ?: return null }
    return AnimeOnTarget(title, translationId = numbers[1], playerId = numbers[2], episode = numbers[0])
}

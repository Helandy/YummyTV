package su.afk.yummy.tv.source.aniboom.data.utils

import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget

private const val PREFIX = "source://aniboom/"
private val ID_PATTERN = Regex("^[A-Za-z0-9]{11}$")

internal fun isAniboomSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://aniboom/{aniboomId}/{episode}/{translation}`. */
internal fun AniboomTarget.toSourceUrl(): String = "$PREFIX$aniboomId/$episode/$translation"

internal fun parseAniboomSourceUrl(url: String): AniboomTarget? {
    if (!isAniboomSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 3) return null
    val aniboomId = parts[0].takeIf { ID_PATTERN.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    val translation = parts[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AniboomTarget(aniboomId, episode, translation)
}

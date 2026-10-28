package su.afk.yummy.tv.source.ailiberty.data.utils

import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyTarget

private const val PREFIX = "source://ailiberty/"
private val SLUG_PATTERN = Regex("^[A-Za-z0-9._-]{1,300}$")

internal fun isAiLibertySourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://ailiberty/{slug}/{episode}`. */
internal fun AiLibertyTarget.toSourceUrl(): String = "$PREFIX$slug/$episode"

internal fun parseAiLibertySourceUrl(url: String): AiLibertyTarget? {
    if (!isAiLibertySourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val slug = parts[0].takeIf { SLUG_PATTERN.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AiLibertyTarget(slug, episode)
}

package su.afk.yummy.tv.source.animevost.data.utils

import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget

private const val PREFIX = "source://animevost/"

private val TYPE_PATH = Regex("^[a-z0-9-]{1,40}$")

/** Slug страницы тайтла: `3640-chitose-kun-wa-ramune-bin-no-naka`. */
private val SLUG = Regex("^\\d{1,7}-[A-Za-z0-9_-]{1,300}$")

internal fun isAnimevostSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://animevost/{раздел}/{slug}/{episode}`. */
internal fun AnimevostTarget.toSourceUrl(): String = "$PREFIX$typePath/$slug/$episode"

internal fun parseAnimevostSourceUrl(url: String): AnimevostTarget? {
    if (!isAnimevostSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 3) return null
    val type = parts[0].takeIf { TYPE_PATH.matches(it) } ?: return null
    val slug = parts[1].takeIf { SLUG.matches(it) } ?: return null
    val episode = parts[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AnimevostTarget(type, slug, episode)
}

/** Раздел и slug из адреса страницы, который отдаёт сервер (`https://animevost.org/tip/tv/3640-….html`); хост зеркальный и не важен. */
internal fun animevostTitleRef(pageUrl: String): Pair<String, String>? {
    val match = Regex("""/tip/([a-z0-9-]{1,40})/(\d{1,7}-[A-Za-z0-9_-]{1,300})\.html""").find(pageUrl) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}

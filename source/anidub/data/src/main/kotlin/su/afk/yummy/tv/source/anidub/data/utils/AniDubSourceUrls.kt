package su.afk.yummy.tv.source.anidub.data.utils

import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget

private const val PREFIX = "source://anidub/"

/** Slug страницы тайтла на сайте: `12254-blich-tysjacheletnjaja-krovavaja-vojna-zakljuchitelnaja-chast`. */
private val SLUG_PATTERN = Regex("^\\d{1,7}-[A-Za-z0-9_-]{1,300}$")

internal fun isAniDubSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://anidub/{slug}/{episode}`. */
internal fun AniDubTarget.toSourceUrl(): String = "$PREFIX$slug/$episode"

internal fun parseAniDubSourceUrl(url: String): AniDubTarget? {
    if (!isAniDubSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val slug = parts[0].takeIf { SLUG_PATTERN.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AniDubTarget(slug, episode)
}

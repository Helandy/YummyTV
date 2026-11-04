package su.afk.yummy.tv.source.studiomir.data.utils

import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget

private const val PREFIX = "source://studiomir/"

/** Slug страницы тайтла на сайте: `12254-blich-tysjacheletnjaja-krovavaja-vojna-zakljuchitelnaja-chast`. */
private val SLUG_PATTERN = Regex("^\\d{1,7}-[A-Za-z0-9_-]{1,300}$")

internal fun isStudioMirSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://studiomir/{slug}/{episode}`. */
internal fun StudioMirTarget.toSourceUrl(): String = "$PREFIX$slug/$episode"

internal fun parseStudioMirSourceUrl(url: String): StudioMirTarget? {
    if (!isStudioMirSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val slug = parts[0].takeIf { SLUG_PATTERN.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return StudioMirTarget(slug, episode)
}

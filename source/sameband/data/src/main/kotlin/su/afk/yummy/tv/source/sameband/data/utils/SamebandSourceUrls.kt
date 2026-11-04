package su.afk.yummy.tv.source.sameband.data.utils

import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget

private const val PREFIX = "source://sameband/"

/** Slug страницы тайтла на сайте: `1234-grand-blue-3`. */
private val SLUG_PATTERN = Regex("^\\d{1,7}-[A-Za-z0-9_-]{1,300}$")

internal fun isSamebandSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://sameband/{slug}/{episode}`. */
internal fun SamebandTarget.toSourceUrl(): String = "$PREFIX$slug/$episode"

internal fun parseSamebandSourceUrl(url: String): SamebandTarget? {
    if (!isSamebandSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val slug = parts[0].takeIf { SLUG_PATTERN.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return SamebandTarget(slug, episode)
}

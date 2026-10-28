package su.afk.yummy.tv.source.dreamerscast.data.utils

import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget

private const val PREFIX = "source://dreamerscast/"

/** Slug страницы релиза: `618-tensei-goblin-dakedo-shitsumon-aru`. */
private val SLUG = Regex("^\\d{1,7}-[A-Za-z0-9_-]{1,300}$")
private val PAGE_URL = Regex("""/home/release/(\d{1,7}-[A-Za-z0-9_-]{1,300})/?$""")

internal fun isDreamerscastSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://dreamerscast/{slug}/{episode}`. */
internal fun DreamerscastTarget.toSourceUrl(): String = "$PREFIX$slug/$episode"

internal fun parseDreamerscastSourceUrl(url: String): DreamerscastTarget? {
    if (!isDreamerscastSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val slug = parts[0].takeIf { SLUG.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return DreamerscastTarget(slug, episode)
}

/** Slug релиза из адреса страницы, который отдаёт сервер (`https://dreamerscast.com/home/release/{slug}`); хост не важен. */
internal fun dreamerscastSlug(pageUrl: String): String? = PAGE_URL.find(pageUrl)?.groupValues?.get(1)

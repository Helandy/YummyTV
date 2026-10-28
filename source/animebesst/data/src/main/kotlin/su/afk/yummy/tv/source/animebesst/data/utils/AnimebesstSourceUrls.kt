package su.afk.yummy.tv.source.animebesst.data.utils

import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget

private const val PREFIX = "source://animebesst/"

/** Slug страницы тайтла: `957-kikansha-no-mahou-wa-tokubetsu-desu-2nd-season`. */
private val SLUG = Regex("^\\d{1,7}-[A-Za-z0-9_-]{1,300}$")
private val HOST = Regex("^[a-z0-9][a-z0-9.-]{2,100}$")
private val PAGE_URL = Regex("""^https://([a-z0-9][a-z0-9.-]{2,100})/anime1-online/(\d{1,7}-[A-Za-z0-9_-]{1,300})\.html$""")

internal fun isAnimebesstSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://animebesst/{slug}/{episode}?h=host1,host2`: хосты сайта в порядке пробы. */
internal fun AnimebesstTarget.toSourceUrl(): String = "$PREFIX$slug/$episode?h=${hosts.joinToString(",")}"

internal fun parseAnimebesstSourceUrl(url: String): AnimebesstTarget? {
    if (!isAnimebesstSourceUrl(url)) return null
    val body = url.substring(PREFIX.length)
    val path = body.substringBefore('?')
    val hosts = body.substringAfter("?h=", "").split(',').filter { it.isNotEmpty() }
    if (hosts.isEmpty() || hosts.any { !HOST.matches(it) }) return null
    val parts = path.split('/')
    if (parts.size != 2) return null
    val slug = parts[0].takeIf { SLUG.matches(it) } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AnimebesstTarget(slug, episode, hosts)
}

/**
 * Slug и хосты из адреса страницы тайтла и её зеркал (`https://anime1.best/anime1-online/{slug}.html`): основной хост первым,
 * повторы убраны. `null`, если основной адрес не похож на страницу тайтла; негодные зеркала пропускаются.
 */
internal fun animebesstTitleRef(pageUrl: String, mirrors: List<String>): Pair<String, List<String>>? {
    val main = PAGE_URL.find(pageUrl) ?: return null
    val slug = main.groupValues[2]
    val hosts = (listOf(main.groupValues[1]) + mirrors.mapNotNull { m ->
        PAGE_URL.find(m)?.takeIf { it.groupValues[2] == slug }?.groupValues?.get(1)
    }).distinct()
    return slug to hosts
}

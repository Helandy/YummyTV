package su.afk.yummy.tv.source.anistar.data.utils

import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget

private const val PREFIX = "source://anistar/"

internal fun isAnistarSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://anistar/{newsId}/{episode}`; зеркала в адресе нет: оно ротируется, а сохранённая ссылка должна жить дольше. */
internal fun AnistarTarget.toSourceUrl(): String = "$PREFIX$newsId/$episode"

internal fun parseAnistarSourceUrl(url: String): AnistarTarget? {
    if (!isAnistarSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val newsId = parts[0].toLongOrNull()?.takeIf { it > 0 } ?: return null
    val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AnistarTarget(newsId, episode)
}

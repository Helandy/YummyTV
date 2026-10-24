package su.afk.yummy.tv.core.utils.poster

import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.utils.network.toHttpsUrlOrNull

/**
 * Собирает [AnimePoster] из версий постера в DTO или кэше: ссылки приводятся к https, пустые
 * отбрасываются. Если ни одной ссылки нет, постера нет — `null`.
 */
fun animePosterOrNull(
    small: String?,
    medium: String?,
    big: String?,
    fullsize: String?,
    mega: String?,
): AnimePoster? {
    val poster = AnimePoster(
        small = small.toHttpsUrlOrNull(),
        medium = medium.toHttpsUrlOrNull(),
        big = big.toHttpsUrlOrNull(),
        fullsize = fullsize.toHttpsUrlOrNull(),
        mega = mega.toHttpsUrlOrNull(),
    )
    return poster.takeUnless {
        it.small == null && it.medium == null && it.big == null && it.fullsize == null && it.mega == null
    }
}

package su.afk.yummy.tv.source.amedia.data.utils

import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget

private const val PREFIX = "source://amedia/"

internal fun isAmediaSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://amedia/{vodId}`: id записи в плеере однозначно задаёт серию, номер и тайтл в адресе не нужны. */
internal fun AmediaTarget.toSourceUrl(): String = "$PREFIX$vodId"

internal fun parseAmediaSourceUrl(url: String): AmediaTarget? {
    if (!isAmediaSourceUrl(url)) return null
    val vodId = url.substring(PREFIX.length).toLongOrNull()?.takeIf { it > 0 } ?: return null
    return AmediaTarget(vodId)
}

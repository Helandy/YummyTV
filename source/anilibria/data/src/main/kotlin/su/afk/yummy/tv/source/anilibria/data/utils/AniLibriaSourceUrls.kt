package su.afk.yummy.tv.source.anilibria.data.utils

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaTarget

private const val PREFIX = "source://anilibria/"

internal fun isAniLibriaSourceUrl(url: String): Boolean = url.startsWith(PREFIX, ignoreCase = true)

/** `source://anilibria/{releaseId}/{ordinal}`. */
internal fun AniLibriaTarget.toSourceUrl(): String = "$PREFIX$releaseId/$ordinal"

internal fun parseAniLibriaSourceUrl(url: String): AniLibriaTarget? {
    if (!isAniLibriaSourceUrl(url)) return null
    val parts = url.substring(PREFIX.length).split('/')
    if (parts.size != 2) return null
    val releaseId = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
    val ordinal = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return AniLibriaTarget(releaseId, ordinal)
}

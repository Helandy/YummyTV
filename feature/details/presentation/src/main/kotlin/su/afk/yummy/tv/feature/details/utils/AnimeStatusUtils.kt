package su.afk.yummy.tv.feature.details.utils

import java.util.Locale

fun String?.isReleasedAnimeStatus(): Boolean =
    this?.trim()?.lowercase(Locale.ROOT) in RELEASED_STATUSES

private val RELEASED_STATUSES = setOf(
    "released",
    "вышел",
)

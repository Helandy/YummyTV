package su.afk.yummy.tv.feature.player.utils

import java.util.Locale

internal fun String?.isOngoingAnimeStatus(): Boolean =
    this?.trim()?.lowercase(Locale.ROOT) in ONGOING_ANIME_STATUSES

private val ONGOING_ANIME_STATUSES = setOf("ongoing", "онгоинг")

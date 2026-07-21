package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem

internal fun UserAnimeListItem.updatedAtMillis(fallback: Long): Long =
    updatedAtSeconds?.takeIf { it > 0L }?.let { it * 1_000L } ?: fallback

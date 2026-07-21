package su.afk.yummy.tv.feature.details.rating.utils

import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.UserAnimeList

internal fun AnimeListStats.count(list: UserAnimeList): Int = counts[list.id] ?: 0

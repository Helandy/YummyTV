package su.afk.yummy.tv.feature.details.details.model

import su.afk.yummy.tv.domain.account.model.UserAnimeList

/** Сколько пользователей держат тайтл в списке [list]. */
data class DetailsListCount(
    val list: UserAnimeList,
    val count: Int,
)

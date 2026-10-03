package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem

internal data class RemoteListItem(
    val list: UserAnimeList,
    val item: UserAnimeListItem,
)

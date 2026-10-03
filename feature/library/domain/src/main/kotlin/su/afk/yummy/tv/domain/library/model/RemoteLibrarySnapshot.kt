package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem

internal data class RemoteLibrarySnapshot(
    val lists: Map<UserAnimeList, List<UserAnimeListItem>>,
    val favorites: List<UserAnimeListItem>,
)

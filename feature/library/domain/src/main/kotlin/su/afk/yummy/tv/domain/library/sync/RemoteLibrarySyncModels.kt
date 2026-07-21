package su.afk.yummy.tv.domain.library.sync

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem

internal data class RemoteLibrarySnapshot(
    val lists: Map<UserAnimeList, List<UserAnimeListItem>>,
    val favorites: List<UserAnimeListItem>,
)

internal data class RemoteListItem(
    val list: UserAnimeList,
    val item: UserAnimeListItem,
)

internal data class LocalLibraryPushResult(
    val changedRemote: Boolean,
    val error: Throwable?,
)

package su.afk.yummy.tv.domain.library.sync

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetAllUserAnimeListsUseCase
import javax.inject.Inject

internal class RemoteLibrarySnapshotLoader @Inject constructor(
    private val getAllUserAnimeLists: GetAllUserAnimeListsUseCase,
) {

    suspend fun load(userId: Int, forceRefresh: Boolean): RemoteLibrarySnapshot {
        val items = getAllUserAnimeLists(userId, forceRefresh)
        return RemoteLibrarySnapshot(
            lists = SYNCED_LISTS.associateWith { list -> items.filter { it.list == list } },
            favorites = items.filter(UserAnimeListItem::isFavorite),
        )
    }

    private companion object {
        val SYNCED_LISTS = listOf(
            UserAnimeList.WATCHING,
            UserAnimeList.PLANNED,
            UserAnimeList.COMPLETED,
            UserAnimeList.POSTPONED,
            UserAnimeList.DROPPED,
        )
    }
}

package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetAllUserAnimeListsUseCase
import su.afk.yummy.tv.domain.library.model.RemoteLibrarySnapshot
import su.afk.yummy.tv.domain.library.utils.SYNCED_LISTS
import javax.inject.Inject

/** Загружает удалённые списки аккаунта и избранное в виде снимка для синхронизации библиотеки. */
internal class LoadRemoteLibrarySnapshotUseCase @Inject constructor(
    private val getAllUserAnimeLists: GetAllUserAnimeListsUseCase,
) {

    suspend operator fun invoke(userId: Int, forceRefresh: Boolean): RemoteLibrarySnapshot {
        val items = getAllUserAnimeLists(userId, forceRefresh)
        return RemoteLibrarySnapshot(
            lists = SYNCED_LISTS.associateWith { list -> items.filter { it.list == list } },
            favorites = items.filter(UserAnimeListItem::isFavorite),
        )
    }

}

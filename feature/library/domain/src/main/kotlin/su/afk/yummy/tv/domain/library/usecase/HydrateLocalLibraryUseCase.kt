package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.RemoteLibrarySnapshot
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.toLibraryItem
import su.afk.yummy.tv.domain.library.utils.updatedAtMillis
import javax.inject.Inject

/**
 * Приводит локальную библиотеку к удалённому снимку: добавляет и обновляет записи и, при
 * [pruneMissingLocalEntries], удаляет локальные, которых на сервере больше нет.
 */
internal class HydrateLocalLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val clock: AppClock,
) {

    suspend operator fun invoke(
        remote: RemoteLibrarySnapshot,
        pruneMissingLocalEntries: Boolean,
        remoteFetchedAt: Long,
    ) {
        val now = clock.nowMillis()
        val localByAnimeId = libraryRepository.getAll()
            .associateBy(LibraryItem::animeId)
            .toMutableMap()
        val remoteAnimeIds = mutableSetOf<Int>()
        val remotePrimaryAnimeIds = remote.lists.values.flatten()
            .mapTo(mutableSetOf(), UserAnimeListItem::animeId)
        val remoteFavoritesByAnimeId = remote.favorites.associateBy(UserAnimeListItem::animeId)
        val remoteFavoriteAnimeIds = remote.lists.values
            .flatten()
            .filter(UserAnimeListItem::isFavorite)
            .mapTo(mutableSetOf(), UserAnimeListItem::animeId)
            .apply { addAll(remoteFavoritesByAnimeId.keys) }

        remote.lists.forEach { (list, items) ->
            items.forEach { remoteItem ->
                remoteAnimeIds += remoteItem.animeId
                val current = localByAnimeId[remoteItem.animeId]
                val merged = remoteItem.toLibraryItem(
                    current = current,
                    now = now,
                    listId = remoteItem.list?.id ?: list.id,
                    isFavorite = if (pruneMissingLocalEntries) {
                        remoteItem.animeId in remoteFavoriteAnimeIds
                    } else {
                        current?.isFavorite == true || remoteItem.isFavorite
                    },
                    listUpdatedAt = remoteItem.updatedAtMillis(remoteFetchedAt),
                    favoriteUpdatedAt = remoteFavoritesByAnimeId[remoteItem.animeId]
                        ?.updatedAtMillis(remoteFetchedAt)
                        ?: remoteItem.takeIf(UserAnimeListItem::isFavorite)
                            ?.updatedAtMillis(remoteFetchedAt)
                        ?: current?.favoriteUpdatedAt
                        ?: 0L,
                )
                localByAnimeId[remoteItem.animeId] = merged
                libraryRepository.add(merged)
            }
        }

        remote.favorites.forEach { remoteFavorite ->
            remoteAnimeIds += remoteFavorite.animeId
            val current = localByAnimeId[remoteFavorite.animeId]
            val merged = remoteFavorite.toLibraryItem(
                current = current,
                now = now,
                listId = if (
                    pruneMissingLocalEntries &&
                    remoteFavorite.animeId !in remotePrimaryAnimeIds
                ) {
                    FAVORITE_ONLY_LIBRARY_LIST_ID
                } else {
                    current?.listId ?: FAVORITE_ONLY_LIBRARY_LIST_ID
                },
                isFavorite = true,
                listUpdatedAt = current?.listUpdatedAt
                    ?: remoteFavorite.updatedAtMillis(remoteFetchedAt),
                favoriteUpdatedAt = remoteFavorite.updatedAtMillis(remoteFetchedAt),
            )
            localByAnimeId[remoteFavorite.animeId] = merged
            libraryRepository.add(merged)
        }

        if (pruneMissingLocalEntries) {
            localByAnimeId.keys
                .filterNot(remoteAnimeIds::contains)
                .forEach { animeId -> libraryRepository.delete(animeId) }
        }
    }
}

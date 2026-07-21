package su.afk.yummy.tv.domain.library.sync

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.updatedAtMillis
import javax.inject.Inject

internal class RemoteLibraryHydrator @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {

    suspend fun hydrate(
        remote: RemoteLibrarySnapshot,
        pruneMissingLocalEntries: Boolean,
        remoteFetchedAt: Long,
    ) {
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

    private companion object {
        fun UserAnimeListItem.toLibraryItem(
            current: LibraryItem?,
            listId: Int,
            isFavorite: Boolean,
            listUpdatedAt: Long,
            favoriteUpdatedAt: Long,
        ): LibraryItem = LibraryItem(
            animeId = animeId,
            title = title.ifBlank { current?.title.orEmpty() },
            poster = LibraryPoster(
                small = poster?.small ?: current?.poster?.small,
                medium = poster?.medium ?: posterUrl ?: current?.poster?.medium,
                big = poster?.big ?: current?.poster?.big,
                fullsize = poster?.fullsize ?: current?.poster?.fullsize,
                mega = poster?.mega ?: current?.poster?.mega,
            ),
            addedAt = current?.addedAt ?: System.currentTimeMillis(),
            listId = listId,
            isFavorite = isFavorite,
            listUpdatedAt = listUpdatedAt,
            favoriteUpdatedAt = if (isFavorite) {
                favoriteUpdatedAt
            } else {
                current?.favoriteUpdatedAt ?: 0L
            },
            userRating = userRating ?: current?.userRating,
        )
    }
}

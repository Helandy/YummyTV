package su.afk.yummy.tv.feature.library.handler

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetAllUserAnimeListsUseCase
import su.afk.yummy.tv.domain.account.usecase.HasCachedUserListsUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.feature.library.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.utils.LocalLibrarySyncResult
import su.afk.yummy.tv.feature.library.utils.userAnimeList
import javax.inject.Inject

/** Loads remote library tabs and syncs local-only library mutations back to the account. */
internal class RemoteLibrarySyncHandler @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val getAllUserAnimeLists: GetAllUserAnimeListsUseCase,
    private val hasCachedUserLists: HasCachedUserListsUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
) {
    suspend fun loadRemoteLists(
        userId: Int,
        forceRefresh: Boolean = false,
    ): RemoteLibraryLoadResult {
        return try {
            val hasKnownRemoteState =
                libraryRepository.hasSyncState(userId) || hasCachedUserLists(userId)
            val allowLocalMissingUpload = !hasKnownRemoteState
            val remoteFetchedAt = System.currentTimeMillis()
            val remote = fetchRemoteLists(userId, forceRefresh)
            val syncResult = syncLocalChangesToRemote(
                remote = remote,
                allowLocalMissingUpload = allowLocalMissingUpload,
                remoteFetchedAt = remoteFetchedAt,
            )
            val currentRemote = if (syncResult.syncedAny) {
                fetchRemoteLists(userId, forceRefresh = true)
            } else {
                remote
            }
            hydrateRemoteToLocal(
                remote = currentRemote,
                pruneMissingLocalEntries = forceRefresh && !allowLocalMissingUpload,
                remoteFetchedAt = remoteFetchedAt,
            )
            if (syncResult.error == null) {
                libraryRepository.markSynced(userId)
            }
            RemoteLibraryLoadResult.Success(
                syncError = syncResult.error,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            RemoteLibraryLoadResult.Failure(error)
        }
    }

    suspend fun removeRemoteEntry(animeId: Int, target: LibraryRemoveTarget): Result<Unit> =
        try {
            when (target) {
                LibraryRemoveTarget.LIST -> removeAnimeList(animeId)
                LibraryRemoveTarget.FAVORITE -> setAnimeFavorite(animeId, false)
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }

    private suspend fun fetchRemoteLists(
        userId: Int,
        forceRefresh: Boolean,
    ): Map<LibraryTab, List<UserAnimeListItem>> {
        val items = getAllUserAnimeLists(userId, forceRefresh)
        return mapOf(
            LibraryTab.WATCHING to items.filter { it.list == UserAnimeList.WATCHING },
            LibraryTab.FAVORITES to items.filter(UserAnimeListItem::isFavorite),
            LibraryTab.PLANNED to items.filter { it.list == UserAnimeList.PLANNED },
            LibraryTab.COMPLETED to items.filter { it.list == UserAnimeList.COMPLETED },
            LibraryTab.POSTPONED to items.filter { it.list == UserAnimeList.POSTPONED },
            LibraryTab.DROPPED to items.filter { it.list == UserAnimeList.DROPPED },
        )
    }

    private suspend fun syncLocalChangesToRemote(
        remote: Map<LibraryTab, List<UserAnimeListItem>>,
        allowLocalMissingUpload: Boolean,
        remoteFetchedAt: Long,
    ): LocalLibrarySyncResult {
        val remotePrimary = remote
            .filterKeys { it != LibraryTab.FAVORITES }
            .flatMap { (tab, items) ->
                val list = tab.userAnimeList() ?: return@flatMap emptyList()
                items.map { item -> item.animeId to RemoteListItem(list, item) }
            }
            .toMap()
        val remoteFavorites = remote[LibraryTab.FAVORITES].orEmpty()
            .associateBy { it.animeId }
        val localItems = libraryRepository.getAll()
        var syncedAny = false
        var firstError: Throwable? = null

        localItems.forEach { entry ->
            val localList = entry.userAnimeList()
            val remoteList = remotePrimary[entry.animeId]
            if (localList != null && shouldPushList(
                    entry = entry,
                    localList = localList,
                    remoteList = remoteList,
                    allowLocalMissingUpload = allowLocalMissingUpload,
                    remoteFetchedAt = remoteFetchedAt,
                )
            ) {
                runCatching {
                    setAnimeList(entry.animeId, localList)
                }.fold(
                    onSuccess = { syncedAny = true },
                    onFailure = { if (firstError == null) firstError = it },
                )
            }

            val remoteFavorite = remoteFavorites[entry.animeId]
            if (shouldPushFavorite(
                    entry = entry,
                    remoteFavorite = remoteFavorite,
                    allowLocalMissingUpload = allowLocalMissingUpload,
                    remoteFetchedAt = remoteFetchedAt,
                )
            ) {
                runCatching {
                    setAnimeFavorite(entry.animeId, entry.isFavorite)
                }.fold(
                    onSuccess = { syncedAny = true },
                    onFailure = { if (firstError == null) firstError = it },
                )
            }
        }

        return LocalLibrarySyncResult(syncedAny = syncedAny, error = firstError)
    }

    private fun shouldPushList(
        entry: LibraryItem,
        localList: UserAnimeList,
        remoteList: RemoteListItem?,
        allowLocalMissingUpload: Boolean,
        remoteFetchedAt: Long,
    ): Boolean {
        remoteList ?: return allowLocalMissingUpload
        val remoteUpdatedAt = remoteList.item.updatedAtMillis(remoteFetchedAt)
        return remoteList.list != localList && entry.listUpdatedAt > remoteUpdatedAt
    }

    private fun shouldPushFavorite(
        entry: LibraryItem,
        remoteFavorite: UserAnimeListItem?,
        allowLocalMissingUpload: Boolean,
        remoteFetchedAt: Long,
    ): Boolean {
        if (entry.isFavorite && remoteFavorite == null) return allowLocalMissingUpload
        if (remoteFavorite == null) return false

        val remoteUpdatedAt = remoteFavorite.updatedAtMillis(remoteFetchedAt)
        return !entry.isFavorite && entry.favoriteUpdatedAt > remoteUpdatedAt
    }

    private suspend fun hydrateRemoteToLocal(
        remote: Map<LibraryTab, List<UserAnimeListItem>>,
        pruneMissingLocalEntries: Boolean,
        remoteFetchedAt: Long,
    ) {
        val merged = libraryRepository.getAll()
            .associateBy { it.animeId }
            .toMutableMap()
        val remoteAnimeIds = mutableSetOf<Int>()
        val remotePrimaryAnimeIds = remote
            .filterKeys { it != LibraryTab.FAVORITES }
            .values
            .flatten()
            .map { it.animeId }
            .toSet()
        val remoteFavoriteItems = remote[LibraryTab.FAVORITES].orEmpty()
            .associateBy { it.animeId }
        val remoteFavoriteAnimeIds = remote.values
            .flatten()
            .filter { it.isFavorite }
            .map { it.animeId }
            .toMutableSet()
            .apply { addAll(remoteFavoriteItems.keys) }

        remote
            .filterKeys { it != LibraryTab.FAVORITES }
            .forEach { (tab, items) ->
                val list = tab.userAnimeList() ?: return@forEach
                items.forEach { item ->
                    remoteAnimeIds += item.animeId
                    val current = merged[item.animeId]
                    val next = item.toLibraryItem(
                        current = current,
                        listId = item.list?.id ?: list.id,
                        isFavorite = if (pruneMissingLocalEntries) {
                            item.animeId in remoteFavoriteAnimeIds
                        } else {
                            current?.isFavorite == true || item.isFavorite
                        },
                        listUpdatedAt = item.updatedAtMillis(remoteFetchedAt),
                        favoriteUpdatedAt = remoteFavoriteItems[item.animeId]
                            ?.updatedAtMillis(remoteFetchedAt)
                            ?: item.takeIf { it.isFavorite }?.updatedAtMillis(remoteFetchedAt)
                            ?: current?.favoriteUpdatedAt
                            ?: 0L,
                    )
                    merged[item.animeId] = next
                    libraryRepository.add(next)
                }
            }

        remote[LibraryTab.FAVORITES].orEmpty().forEach { item ->
            remoteAnimeIds += item.animeId
            val current = merged[item.animeId]
            val next = item.toLibraryItem(
                current = current,
                listId = if (pruneMissingLocalEntries && item.animeId !in remotePrimaryAnimeIds) {
                    FAVORITE_ONLY_LIBRARY_LIST_ID
                } else {
                    current?.listId ?: FAVORITE_ONLY_LIBRARY_LIST_ID
                },
                isFavorite = true,
                listUpdatedAt = current?.listUpdatedAt ?: item.updatedAtMillis(remoteFetchedAt),
                favoriteUpdatedAt = item.updatedAtMillis(remoteFetchedAt),
            )
            merged[item.animeId] = next
            libraryRepository.add(next)
        }

        if (pruneMissingLocalEntries) {
            merged.keys
                .filterNot { it in remoteAnimeIds }
                .forEach { animeId -> libraryRepository.delete(animeId) }
        }
    }

    private fun UserAnimeListItem.toLibraryItem(
        current: LibraryItem?,
        listId: Int,
        isFavorite: Boolean,
        listUpdatedAt: Long,
        favoriteUpdatedAt: Long,
    ): LibraryItem =
        LibraryItem(
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
            favoriteUpdatedAt = if (isFavorite) favoriteUpdatedAt else current?.favoriteUpdatedAt
                ?: 0L,
            userRating = userRating ?: current?.userRating,
        )

    private fun UserAnimeListItem.updatedAtMillis(fallback: Long): Long =
        updatedAtSeconds?.takeIf { it > 0L }?.let { it * 1000L } ?: fallback

    private data class RemoteListItem(
        val list: UserAnimeList,
        val item: UserAnimeListItem,
    )
}

/** Result of loading remote library tabs and optional local-to-remote sync. */
internal sealed interface RemoteLibraryLoadResult {
    data class Success(val syncError: Throwable?) : RemoteLibraryLoadResult

    data class Failure(val error: Throwable) : RemoteLibraryLoadResult
}

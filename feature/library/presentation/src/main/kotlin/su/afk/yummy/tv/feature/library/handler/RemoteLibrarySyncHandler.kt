package su.afk.yummy.tv.feature.library.handler

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.storage.library.FAVORITE_ONLY_LIST_ID
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.feature.library.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.utils.LocalLibrarySyncResult
import su.afk.yummy.tv.feature.library.utils.userAnimeList
import javax.inject.Inject

/** Loads remote library tabs and syncs local-only library mutations back to the account. */
internal class RemoteLibrarySyncHandler @Inject constructor(
    private val libraryStore: LibraryStore,
    private val getUserAnimeList: GetUserAnimeListUseCase,
    private val getUserFavoriteAnimeList: GetUserFavoriteAnimeListUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
) {
    suspend fun loadRemoteLists(userId: Int): RemoteLibraryLoadResult {
        return try {
            val remote = fetchRemoteLists(userId)
            val syncResult = syncLocalMissingToRemote(remote)
            val currentRemote = if (syncResult.syncedAny) fetchRemoteLists(userId) else remote
            hydrateRemoteToLocal(currentRemote)
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

    private suspend fun fetchRemoteLists(userId: Int): Map<LibraryTab, List<UserAnimeListItem>> =
        coroutineScope {
            val watching = async { getUserAnimeList(userId, UserAnimeList.WATCHING) }
            val favorites = async { getUserFavoriteAnimeList(userId) }
            val planned = async { getUserAnimeList(userId, UserAnimeList.PLANNED) }
            val completed = async { getUserAnimeList(userId, UserAnimeList.COMPLETED) }
            val postponed = async { getUserAnimeList(userId, UserAnimeList.POSTPONED) }
            val dropped = async { getUserAnimeList(userId, UserAnimeList.DROPPED) }

            mapOf(
                LibraryTab.WATCHING to watching.await(),
                LibraryTab.FAVORITES to favorites.await(),
                LibraryTab.PLANNED to planned.await(),
                LibraryTab.COMPLETED to completed.await(),
                LibraryTab.POSTPONED to postponed.await(),
                LibraryTab.DROPPED to dropped.await(),
            )
        }

    private suspend fun syncLocalMissingToRemote(
        remote: Map<LibraryTab, List<UserAnimeListItem>>,
    ): LocalLibrarySyncResult {
        val remotePrimaryAnimeIds = remote
            .filterKeys { it != LibraryTab.FAVORITES }
            .values
            .flatten()
            .map { it.animeId }
            .toSet()
        val remoteFavoriteAnimeIds =
            remote[LibraryTab.FAVORITES].orEmpty().map { it.animeId }.toSet()
        val localItems = libraryStore.getAll()
        val localMissing = localItems
            .filter { it.listId >= 0 }
            .filterNot { it.animeId in remotePrimaryAnimeIds }
        var syncedAny = false
        var firstError: Throwable? = null

        localMissing.forEach { entry ->
            val list = entry.userAnimeList() ?: return@forEach
            runCatching {
                setAnimeList(entry.animeId, list)
            }.fold(
                onSuccess = { syncedAny = true },
                onFailure = { if (firstError == null) firstError = it },
            )
        }

        localItems
            .filter { it.isFavorite }
            .filterNot { it.animeId in remoteFavoriteAnimeIds }
            .forEach { entry ->
                runCatching {
                    setAnimeFavorite(entry.animeId, true)
                }.fold(
                    onSuccess = { syncedAny = true },
                    onFailure = { if (firstError == null) firstError = it },
                )
            }

        return LocalLibrarySyncResult(syncedAny = syncedAny, error = firstError)
    }

    private suspend fun hydrateRemoteToLocal(remote: Map<LibraryTab, List<UserAnimeListItem>>) {
        val merged = libraryStore.getAll()
            .associateBy { it.animeId }
            .toMutableMap()

        remote
            .filterKeys { it != LibraryTab.FAVORITES }
            .forEach { (tab, items) ->
                val list = tab.userAnimeList() ?: return@forEach
                items.forEach { item ->
                    val current = merged[item.animeId]
                    val next = item.toLibraryEntry(
                        current = current,
                        listId = item.list?.id ?: list.id,
                        isFavorite = current?.isFavorite == true || item.isFavorite,
                    )
                    merged[item.animeId] = next
                    libraryStore.add(next)
                }
            }

        remote[LibraryTab.FAVORITES].orEmpty().forEach { item ->
            val current = merged[item.animeId]
            val next = item.toLibraryEntry(
                current = current,
                listId = current?.listId ?: FAVORITE_ONLY_LIST_ID,
                isFavorite = true,
            )
            merged[item.animeId] = next
            libraryStore.add(next)
        }
    }

    private fun UserAnimeListItem.toLibraryEntry(
        current: LibraryEntry?,
        listId: Int,
        isFavorite: Boolean,
    ): LibraryEntry =
        LibraryEntry(
            animeId = animeId,
            title = title.ifBlank { current?.title.orEmpty() },
            posterSmallUrl = poster?.small ?: current?.posterSmallUrl,
            posterMediumUrl = poster?.medium ?: posterUrl ?: current?.posterMediumUrl,
            posterBigUrl = poster?.big ?: current?.posterBigUrl,
            posterFullsizeUrl = poster?.fullsize ?: current?.posterFullsizeUrl,
            posterMegaUrl = poster?.mega ?: current?.posterMegaUrl,
            addedAt = current?.addedAt ?: System.currentTimeMillis(),
            listId = listId,
            isFavorite = isFavorite,
        )
}

/** Result of loading remote library tabs and optional local-to-remote sync. */
internal sealed interface RemoteLibraryLoadResult {
    data class Success(val syncError: Throwable?) : RemoteLibraryLoadResult

    data class Failure(val error: Throwable) : RemoteLibraryLoadResult
}

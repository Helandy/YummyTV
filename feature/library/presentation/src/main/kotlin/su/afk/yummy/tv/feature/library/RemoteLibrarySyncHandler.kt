package su.afk.yummy.tv.feature.library

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
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
            RemoteLibraryLoadResult.Success(
                remoteItems = currentRemote,
                syncError = syncResult.error,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            RemoteLibraryLoadResult.Failure(error.message)
        }
    }

    suspend fun removeRemoteEntry(animeId: Int, favorite: Boolean): Result<Unit> =
        runCatching {
            if (favorite) {
                setAnimeFavorite(animeId, false)
                libraryStore.setFavorite(animeId, title = "", poster = null, favorite = false)
            } else {
                removeAnimeList(animeId)
                libraryStore.remove(animeId)
            }
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
        var firstError: String? = null

        localMissing.forEach { entry ->
            val list = entry.userAnimeList() ?: return@forEach
            runCatching {
                setAnimeList(entry.animeId, list)
            }.fold(
                onSuccess = { syncedAny = true },
                onFailure = { if (firstError == null) firstError = it.message },
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
                    onFailure = { if (firstError == null) firstError = it.message },
                )
            }

        return LocalLibrarySyncResult(syncedAny = syncedAny, error = firstError)
    }
}

/** Result of loading remote library tabs and optional local-to-remote sync. */
internal sealed interface RemoteLibraryLoadResult {
    data class Success(
        val remoteItems: Map<LibraryTab, List<UserAnimeListItem>>,
        val syncError: String?,
    ) : RemoteLibraryLoadResult

    data class Failure(val message: String?) : RemoteLibraryLoadResult
}

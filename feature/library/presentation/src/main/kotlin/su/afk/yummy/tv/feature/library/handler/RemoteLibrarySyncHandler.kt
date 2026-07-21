package su.afk.yummy.tv.feature.library.handler

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoteLibrarySyncResult
import su.afk.yummy.tv.domain.library.usecase.SyncRemoteLibraryUseCase
import su.afk.yummy.tv.feature.library.LibraryRemoveTarget
import javax.inject.Inject

/** Adapts domain library synchronization and remote mutations to presentation events. */
internal class RemoteLibrarySyncHandler @Inject constructor(
    private val syncRemoteLibrary: SyncRemoteLibraryUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
) {
    suspend fun loadRemoteLists(
        userId: Int,
        forceRefresh: Boolean = false,
    ): RemoteLibrarySyncResult = syncRemoteLibrary(userId, forceRefresh)

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
}

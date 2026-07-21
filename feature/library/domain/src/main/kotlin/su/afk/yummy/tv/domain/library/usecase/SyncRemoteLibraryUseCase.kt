package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.account.usecase.HasCachedUserListsUseCase
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.sync.LocalLibraryChangePusher
import su.afk.yummy.tv.domain.library.sync.RemoteLibraryHydrator
import su.afk.yummy.tv.domain.library.sync.RemoteLibrarySnapshotLoader
import javax.inject.Inject

/** Reconciles the account library with local library state without exposing storage to UI. */
class SyncRemoteLibraryUseCase @Inject internal constructor(
    private val libraryRepository: LibraryRepository,
    private val hasCachedUserLists: HasCachedUserListsUseCase,
    private val snapshotLoader: RemoteLibrarySnapshotLoader,
    private val localChangePusher: LocalLibraryChangePusher,
    private val remoteHydrator: RemoteLibraryHydrator,
) {

    suspend operator fun invoke(
        userId: Int,
        forceRefresh: Boolean = false,
    ): RemoteLibrarySyncResult = try {
        val hasKnownRemoteState =
            libraryRepository.hasSyncState(userId) || hasCachedUserLists(userId)
        val allowMissingRemoteUpload = !hasKnownRemoteState
        val remoteFetchedAt = System.currentTimeMillis()
        val initialRemote = snapshotLoader.load(userId, forceRefresh)
        val pushResult = localChangePusher.push(
            remote = initialRemote,
            allowMissingRemoteUpload = allowMissingRemoteUpload,
            remoteFetchedAt = remoteFetchedAt,
        )
        val resolvedRemote = if (pushResult.changedRemote) {
            snapshotLoader.load(userId, forceRefresh = true)
        } else {
            initialRemote
        }

        remoteHydrator.hydrate(
            remote = resolvedRemote,
            pruneMissingLocalEntries = forceRefresh && hasKnownRemoteState,
            remoteFetchedAt = remoteFetchedAt,
        )
        if (pushResult.error == null) {
            libraryRepository.markSynced(userId)
        }
        RemoteLibrarySyncResult.Success(syncError = pushResult.error)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        RemoteLibrarySyncResult.Failure(error)
    }
}

sealed interface RemoteLibrarySyncResult {
    data class Success(val syncError: Throwable?) : RemoteLibrarySyncResult
    data class Failure(val error: Throwable) : RemoteLibrarySyncResult
}

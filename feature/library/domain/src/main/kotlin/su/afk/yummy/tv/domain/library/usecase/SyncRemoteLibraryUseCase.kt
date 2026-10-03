package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import su.afk.yummy.tv.domain.account.usecase.HasCachedUserListsUseCase
import su.afk.yummy.tv.domain.library.model.RemoteLibrarySyncResult
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/** Reconciles the account library with local library state without exposing storage to UI. */
class SyncRemoteLibraryUseCase @Inject internal constructor(
    private val libraryRepository: LibraryRepository,
    private val hasCachedUserLists: HasCachedUserListsUseCase,
    private val loadRemoteSnapshot: LoadRemoteLibrarySnapshotUseCase,
    private val pushLocalChanges: PushLocalLibraryChangesUseCase,
    private val hydrateLocalLibrary: HydrateLocalLibraryUseCase,
    private val clock: AppClock,
) {

    suspend operator fun invoke(
        userId: Int,
        forceRefresh: Boolean = false,
    ): RemoteLibrarySyncResult = try {
        val hasKnownRemoteState =
            libraryRepository.hasSyncState(userId) || hasCachedUserLists(userId)
        val allowMissingRemoteUpload = !hasKnownRemoteState
        val remoteFetchedAt = clock.nowMillis()
        val initialRemote = loadRemoteSnapshot(userId, forceRefresh)
        val pushResult = pushLocalChanges(
            remote = initialRemote,
            allowMissingRemoteUpload = allowMissingRemoteUpload,
            remoteFetchedAt = remoteFetchedAt,
        )
        val resolvedRemote = if (pushResult.changedRemote) {
            loadRemoteSnapshot(userId, forceRefresh = true)
        } else {
            initialRemote
        }

        hydrateLocalLibrary(
            remote = resolvedRemote,
            pruneMissingLocalEntries = forceRefresh && hasKnownRemoteState,
            remoteFetchedAt = remoteFetchedAt,
        )
        if (pushResult.errors.isEmpty()) {
            libraryRepository.markSynced(userId)
        }
        RemoteLibrarySyncResult.Success(pushErrors = pushResult.errors)
    } catch (error: Throwable) {
        currentCoroutineContext().ensureActive()
        RemoteLibrarySyncResult.Failure(error)
    }
}

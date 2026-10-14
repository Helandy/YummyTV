package su.afk.yummy.tv.domain.library.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.usecase.HasCachedUserListsUseCase
import su.afk.yummy.tv.domain.library.model.LocalLibraryPushResult
import su.afk.yummy.tv.domain.library.model.RemoteLibrarySyncResult
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class SyncRemoteLibraryUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk(relaxed = true)
    private val hasCachedUserLists: HasCachedUserListsUseCase = mockk()
    private val loadRemoteSnapshot: LoadRemoteLibrarySnapshotUseCase = mockk()
    private val pushLocalChanges: PushLocalLibraryChangesUseCase = mockk()
    private val hydrateLocalLibrary: HydrateLocalLibraryUseCase = mockk(relaxed = true)
    private val clock: AppClock = mockk()

    private val initial = snapshot()
    private val refreshed = snapshot(favorites = listOf(remoteItem(1, favorite = true)))

    @Before
    fun setUp() {
        every { clock.nowMillis() } returns NOW
        coEvery { repository.hasSyncState(USER_ID) } returns true
        coEvery { hasCachedUserLists(USER_ID) } returns false
        coEvery { loadRemoteSnapshot(USER_ID, any()) } returns initial
        coEvery { pushLocalChanges(any(), any(), any()) } returns LocalLibraryPushResult(changedRemote = false, errors = emptyList())
    }

    @Test
    fun `first sync uploads local entries and never prunes`() = runTest {
        coEvery { repository.hasSyncState(USER_ID) } returns false

        createUseCase()(USER_ID, forceRefresh = true)

        coVerify { pushLocalChanges(initial, allowMissingRemoteUpload = true, remoteFetchedAt = NOW) }
        coVerify { hydrateLocalLibrary(initial, pruneMissingLocalEntries = false, remoteFetchedAt = NOW) }
    }

    @Test
    fun `forced sync of a known account prunes local entries`() = runTest {
        createUseCase()(USER_ID, forceRefresh = true)

        coVerify { hydrateLocalLibrary(initial, pruneMissingLocalEntries = true, remoteFetchedAt = NOW) }
        coVerify { repository.markSynced(USER_ID) }
    }

    @Test
    fun `pushed changes are re-read before hydrating`() = runTest {
        coEvery { pushLocalChanges(any(), any(), any()) } returns LocalLibraryPushResult(changedRemote = true, errors = emptyList())
        coEvery { loadRemoteSnapshot(USER_ID, true) } returns refreshed

        createUseCase()(USER_ID, forceRefresh = false)

        coVerify { hydrateLocalLibrary(refreshed, any(), any()) }
    }

    @Test
    fun `push errors are reported and keep the account unsynced`() = runTest {
        val error = IllegalStateException("500")
        coEvery { pushLocalChanges(any(), any(), any()) } returns LocalLibraryPushResult(changedRemote = false, errors = listOf(error))

        val result = createUseCase()(USER_ID)

        assertEquals(RemoteLibrarySyncResult.Success(pushErrors = listOf(error)), result)
        coVerify(exactly = 0) { repository.markSynced(any()) }
    }

    @Test
    fun `failed snapshot load is a sync failure`() = runTest {
        coEvery { loadRemoteSnapshot(USER_ID, any()) } throws IllegalStateException("network")

        assertTrue(createUseCase()(USER_ID) is RemoteLibrarySyncResult.Failure)
    }

    private fun createUseCase() = SyncRemoteLibraryUseCase(
        libraryRepository = repository,
        hasCachedUserLists = hasCachedUserLists,
        loadRemoteSnapshot = loadRemoteSnapshot,
        pushLocalChanges = pushLocalChanges,
        hydrateLocalLibrary = hydrateLocalLibrary,
        clock = clock,
    )

    private companion object {
        const val USER_ID = 5
        const val NOW = 1_000_000L
    }
}

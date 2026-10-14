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
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class HydrateLocalLibraryUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk(relaxed = true)
    private val clock: AppClock = mockk()
    private val saved = mutableListOf<LibraryItem>()

    @Before
    fun setUp() {
        every { clock.nowMillis() } returns NOW
        coEvery { repository.add(any()) } answers { saved += firstArg<LibraryItem>() }
    }

    @Test
    fun `remote list entries are stored with the server time`() = runTest {
        coEvery { repository.getAll() } returns emptyList()
        val remote = snapshot(lists = mapOf(UserAnimeList.PLANNED to listOf(remoteItem(1, updatedAtSeconds = 50))))

        createUseCase()(remote, pruneMissingLocalEntries = false, remoteFetchedAt = NOW)

        val item = saved.single()
        assertEquals(UserAnimeList.PLANNED.id, item.listId)
        assertEquals(50_000L, item.listUpdatedAt)
    }

    @Test
    fun `soft sync keeps local-only entries and local favorites`() = runTest {
        coEvery { repository.getAll() } returns listOf(localItem(1, UserAnimeList.WATCHING, favorite = true), localItem(2, UserAnimeList.DROPPED))
        val remote = snapshot(lists = mapOf(UserAnimeList.WATCHING to listOf(remoteItem(1))))

        createUseCase()(remote, pruneMissingLocalEntries = false, remoteFetchedAt = NOW)

        assertTrue(saved.single().isFavorite)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `full sync mirrors the server, removing local-only entries and stale favorites`() = runTest {
        coEvery { repository.getAll() } returns listOf(localItem(1, UserAnimeList.WATCHING, favorite = true), localItem(2, UserAnimeList.DROPPED))
        val remote = snapshot(lists = mapOf(UserAnimeList.WATCHING to listOf(remoteItem(1))))

        createUseCase()(remote, pruneMissingLocalEntries = true, remoteFetchedAt = NOW)

        assertEquals(false, saved.single().isFavorite)
        coVerify { repository.delete(2) }
        coVerify(exactly = 0) { repository.delete(1) }
    }

    @Test
    fun `favorite outside lists is stored as favorite-only on a full sync`() = runTest {
        coEvery { repository.getAll() } returns listOf(localItem(3, UserAnimeList.PLANNED))
        val remote = snapshot(favorites = listOf(remoteItem(3, favorite = true, updatedAtSeconds = 70)))

        createUseCase()(remote, pruneMissingLocalEntries = true, remoteFetchedAt = NOW)

        val item = saved.single()
        assertEquals(FAVORITE_ONLY_LIBRARY_LIST_ID, item.listId)
        assertTrue(item.isFavorite)
        assertEquals(70_000L, item.favoriteUpdatedAt)
    }

    @Test
    fun `favorite keeps its local list on a soft sync`() = runTest {
        coEvery { repository.getAll() } returns listOf(localItem(3, UserAnimeList.PLANNED))
        val remote = snapshot(favorites = listOf(remoteItem(3, favorite = true)))

        createUseCase()(remote, pruneMissingLocalEntries = false, remoteFetchedAt = NOW)

        assertEquals(UserAnimeList.PLANNED.id, saved.single().listId)
    }

    private fun createUseCase() = HydrateLocalLibraryUseCase(repository, clock)

    private companion object {
        const val NOW = 1_000_000L
    }
}

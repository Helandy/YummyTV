package su.afk.yummy.tv.domain.library.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class PushLocalLibraryChangesUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk()
    private val setAnimeList: SetAnimeListUseCase = mockk(relaxed = true)
    private val setAnimeFavorite: SetAnimeFavoriteUseCase = mockk(relaxed = true)

    @Test
    fun `local entry unknown to the server is uploaded only on a first sync`() = runTest {
        local(localItem(1, UserAnimeList.WATCHING), localItem(2, favorite = true))
        val useCase = createUseCase()

        val first = useCase(snapshot(), allowMissingRemoteUpload = true, remoteFetchedAt = NOW)
        coVerify { setAnimeList(1, UserAnimeList.WATCHING) }
        coVerify { setAnimeFavorite(2, true) }
        assertTrue(first.changedRemote)

        val later = useCase(snapshot(), allowMissingRemoteUpload = false, remoteFetchedAt = NOW)
        assertFalse(later.changedRemote)
        coVerify(exactly = 1) { setAnimeList(any(), any()) }
    }

    @Test
    fun `list change is pushed only when the local one is newer`() = runTest {
        // Сервер: COMPLETED в 100 c. Локально: WATCHING.
        val remote = snapshot(lists = mapOf(UserAnimeList.COMPLETED to listOf(remoteItem(1, updatedAtSeconds = 100))))
        val useCase = createUseCase()

        local(localItem(1, UserAnimeList.WATCHING, listUpdatedAt = 99_000L))
        useCase(remote, allowMissingRemoteUpload = false, remoteFetchedAt = NOW)
        coVerify(exactly = 0) { setAnimeList(any(), any()) }

        local(localItem(1, UserAnimeList.WATCHING, listUpdatedAt = 101_000L))
        useCase(remote, allowMissingRemoteUpload = false, remoteFetchedAt = NOW)
        coVerify(exactly = 1) { setAnimeList(1, UserAnimeList.WATCHING) }
    }

    @Test
    fun `same list is not pushed`() = runTest {
        local(localItem(1, UserAnimeList.WATCHING, listUpdatedAt = NOW))
        val remote = snapshot(lists = mapOf(UserAnimeList.WATCHING to listOf(remoteItem(1, updatedAtSeconds = 1))))

        createUseCase()(remote, allowMissingRemoteUpload = false, remoteFetchedAt = NOW)

        coVerify(exactly = 0) { setAnimeList(any(), any()) }
    }

    @Test
    fun `removed favorite is pushed only when removed after the server change`() = runTest {
        val remote = snapshot(favorites = listOf(remoteItem(1, favorite = true, updatedAtSeconds = 100)))
        val useCase = createUseCase()

        local(localItem(1, favorite = false, favoriteUpdatedAt = 101_000L))
        useCase(remote, allowMissingRemoteUpload = false, remoteFetchedAt = NOW)
        coVerify(exactly = 1) { setAnimeFavorite(1, false) }

        local(localItem(1, favorite = false, favoriteUpdatedAt = 50_000L))
        useCase(remote, allowMissingRemoteUpload = false, remoteFetchedAt = NOW)
        coVerify(exactly = 1) { setAnimeFavorite(any(), any()) }
    }

    @Test
    fun `failed pushes are collected and do not count as changes`() = runTest {
        val error = IllegalStateException("500")
        coEvery { setAnimeList(any(), any()) } throws error
        local(localItem(1, UserAnimeList.PLANNED))

        val result = createUseCase()(snapshot(), allowMissingRemoteUpload = true, remoteFetchedAt = NOW)

        assertFalse(result.changedRemote)
        assertEquals(listOf(error), result.errors)
    }

    private fun local(vararg items: LibraryItem) {
        coEvery { repository.getAll() } returns items.toList()
    }

    private fun createUseCase() = PushLocalLibraryChangesUseCase(repository, setAnimeList, setAnimeFavorite)

    private companion object {
        const val NOW = 1_000_000L
    }
}

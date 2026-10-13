package su.afk.yummy.tv.feature.library.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.library.usecase.SyncRemoteLibraryUseCase
import su.afk.yummy.tv.feature.library.model.LibraryRemoveTarget

class RemoteLibrarySyncHandlerTest : BaseUnitTest() {

    private val syncRemoteLibrary: SyncRemoteLibraryUseCase = mockk()
    private val removeAnimeList: RemoveAnimeListUseCase = mockk(relaxed = true)
    private val setAnimeFavorite: SetAnimeFavoriteUseCase = mockk(relaxed = true)

    @Test
    fun `removing from a list and from favorites hit different endpoints`() = runTest {
        val handler = createHandler()

        assertTrue(handler.removeRemoteEntry(ANIME_ID, LibraryRemoveTarget.LIST).isSuccess)
        assertTrue(handler.removeRemoteEntry(ANIME_ID, LibraryRemoveTarget.FAVORITE).isSuccess)

        coVerify { removeAnimeList(ANIME_ID) }
        coVerify { setAnimeFavorite(ANIME_ID, false) }
    }

    @Test
    fun `failed removal is returned as a failure`() = runTest {
        coEvery { removeAnimeList(any()) } throws IllegalStateException("500")

        assertTrue(createHandler().removeRemoteEntry(ANIME_ID, LibraryRemoveTarget.LIST).isFailure)
    }

    private fun createHandler() = RemoteLibrarySyncHandler(syncRemoteLibrary, removeAnimeList, setAnimeFavorite)

    private companion object {
        const val ANIME_ID = 42
    }
}

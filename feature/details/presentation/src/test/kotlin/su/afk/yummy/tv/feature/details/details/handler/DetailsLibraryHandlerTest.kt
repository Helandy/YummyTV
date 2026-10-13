package su.afk.yummy.tv.feature.details.details.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoveLibraryItemUseCase
import su.afk.yummy.tv.domain.library.usecase.SetLibraryFavoriteUseCase
import su.afk.yummy.tv.domain.library.usecase.UpsertLibraryItemUseCase
import su.afk.yummy.tv.feature.details.animeDetails
import java.io.IOException

class DetailsLibraryHandlerTest : BaseUnitTest() {

    private val removeLibraryItem: RemoveLibraryItemUseCase = mockk(relaxed = true)
    private val setLibraryFavorite: SetLibraryFavoriteUseCase = mockk(relaxed = true)
    private val upsertLibraryItem: UpsertLibraryItemUseCase = mockk(relaxed = true)
    private val getAnimeListState: GetAnimeListStateUseCase = mockk()
    private val setAnimeFavorite: SetAnimeFavoriteUseCase = mockk(relaxed = true)
    private val setAnimeList: SetAnimeListUseCase = mockk(relaxed = true)
    private val removeAnimeList: RemoveAnimeListUseCase = mockk(relaxed = true)
    private val pendingMutationQueue: PendingMutationQueue = mockk()

    private val details = animeDetails(id = ANIME_ID, title = "Title")

    @Test
    fun `remote list state is mapped and a missing one is null`() = runTest {
        val handler = createHandler()
        coEvery { getAnimeListState(ANIME_ID) } returns listItem(UserAnimeList.WATCHING, favorite = true)

        assertEquals(
            DetailsLibraryState(isInLibrary = true, libraryList = UserAnimeList.WATCHING, isFavorite = true),
            handler.refreshAuthorizedState(ANIME_ID).getOrNull(),
        )

        coEvery { getAnimeListState(ANIME_ID) } returns null
        assertNull(handler.refreshAuthorizedState(ANIME_ID).getOrThrow())
    }

    @Test
    fun `guest changes only the local library`() = runTest {
        val result = createHandler().addToLibrary(
            ANIME_ID,
            details,
            UserAnimeList.PLANNED,
            wasInLibrary = false,
            previousList = null,
            isFavorite = false,
            isSignedIn = false,
        )

        assertEquals(DetailsLibraryMutationResult.Success, result)
        coVerify { upsertLibraryItem(any()) }
        coVerify(exactly = 0) { setAnimeList(any(), any()) }
    }

    @Test
    fun `added list is sent to the server`() = runTest {
        val result = createHandler().addToLibrary(
            ANIME_ID,
            details,
            UserAnimeList.PLANNED,
            wasInLibrary = false,
            previousList = null,
            isFavorite = false,
            isSignedIn = true,
        )

        assertEquals(DetailsLibraryMutationResult.Success, result)
        coVerify { setAnimeList(ANIME_ID, UserAnimeList.PLANNED) }
    }

    @Test
    fun `offline add is queued and kept locally`() = runTest {
        val error = IOException("offline")
        coEvery { setAnimeList(any(), any()) } throws error
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), any()) } returns true

        val result = createHandler().addToLibrary(
            ANIME_ID,
            details,
            UserAnimeList.PLANNED,
            wasInLibrary = false,
            previousList = null,
            isFavorite = false,
            isSignedIn = true,
        )

        assertEquals(DetailsLibraryMutationResult.Success, result)
        coVerify { pendingMutationQueue.enqueueOnNetworkFailure(PendingMutation.SetList(ANIME_ID, UserAnimeList.PLANNED.id), error) }
        coVerify(exactly = 0) { removeLibraryItem(any()) }
    }

    @Test
    fun `rejected add of a new title removes it locally again`() = runTest {
        rejectedByServer { coEvery { setAnimeList(any(), any()) } throws it }

        val result = createHandler().addToLibrary(
            ANIME_ID,
            details,
            UserAnimeList.PLANNED,
            wasInLibrary = false,
            previousList = null,
            isFavorite = false,
            isSignedIn = true,
        )

        assertEquals(DetailsLibraryMutationResult.RollbackLibrary(isInLibrary = false, libraryList = null), result)
        coVerify { removeLibraryItem(ANIME_ID) }
    }

    @Test
    fun `rejected list change restores the previous list locally`() = runTest {
        rejectedByServer { coEvery { setAnimeList(any(), any()) } throws it }

        val result = createHandler().addToLibrary(
            ANIME_ID,
            details,
            UserAnimeList.COMPLETED,
            wasInLibrary = true,
            previousList = UserAnimeList.WATCHING,
            isFavorite = false,
            isSignedIn = true,
        )

        assertEquals(DetailsLibraryMutationResult.RollbackLibrary(true, UserAnimeList.WATCHING), result)
        coVerify(exactly = 2) { upsertLibraryItem(any()) }
        coVerify(exactly = 0) { removeLibraryItem(any()) }
    }

    @Test
    fun `removing a title without a remote list touches only the local library`() = runTest {
        val result = createHandler().removeFromLibrary(
            ANIME_ID,
            details,
            previousList = null,
            wasInLibrary = true,
            isFavorite = false,
            isSignedIn = true,
        )

        assertEquals(DetailsLibraryMutationResult.Success, result)
        coVerify { removeLibraryItem(ANIME_ID) }
        coVerify(exactly = 0) { removeAnimeList(any()) }
    }

    @Test
    fun `rejected removal puts the title back`() = runTest {
        rejectedByServer { coEvery { removeAnimeList(any()) } throws it }

        val result = createHandler().removeFromLibrary(
            ANIME_ID,
            details,
            previousList = UserAnimeList.DROPPED,
            wasInLibrary = true,
            isFavorite = true,
            isSignedIn = true,
        )

        assertEquals(DetailsLibraryMutationResult.RollbackLibrary(true, UserAnimeList.DROPPED), result)
        coVerify { upsertLibraryItem(any()) }
    }

    @Test
    fun `rejected favorite restores the previous flag`() = runTest {
        rejectedByServer { coEvery { setAnimeFavorite(any(), any()) } throws it }

        val result = createHandler().setFavorite(ANIME_ID, details, favorite = true, previousFavorite = false, isSignedIn = true)

        assertEquals(DetailsLibraryMutationResult.RollbackFavorite(false), result)
        coVerify { setLibraryFavorite(ANIME_ID, "Title", null, null, true) }
        coVerify { setLibraryFavorite(ANIME_ID, "Title", null, null, false) }
    }

    @Test
    fun `offline favorite is queued`() = runTest {
        val error = IOException("offline")
        coEvery { setAnimeFavorite(any(), any()) } throws error
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), any()) } returns true

        val result = createHandler().setFavorite(ANIME_ID, details, favorite = true, previousFavorite = false, isSignedIn = true)

        assertEquals(DetailsLibraryMutationResult.Success, result)
        coVerify { pendingMutationQueue.enqueueOnNetworkFailure(PendingMutation.SetFavorite(ANIME_ID, true), error) }
    }

    /** Сервер отклонил запрос: очередь (только для сетевых ошибок) его не берёт — нужен откат. */
    private fun rejectedByServer(stub: (Throwable) -> Unit) {
        val error = IllegalStateException("400")
        stub(error)
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), error) } returns false
    }

    private fun listItem(list: UserAnimeList?, favorite: Boolean) = UserAnimeListItem(
        animeId = ANIME_ID,
        title = "Title",
        posterUrl = null,
        rating = null,
        year = null,
        list = list,
        isFavorite = favorite,
    )

    private fun createHandler() = DetailsLibraryHandler(
        removeLibraryItem = removeLibraryItem,
        setLibraryFavorite = setLibraryFavorite,
        upsertLibraryItem = upsertLibraryItem,
        getAnimeListState = getAnimeListState,
        setAnimeFavorite = setAnimeFavorite,
        setAnimeList = setAnimeList,
        removeAnimeList = removeAnimeList,
        pendingMutationQueue = pendingMutationQueue,
    )

    private companion object {
        const val ANIME_ID = 1
    }
}

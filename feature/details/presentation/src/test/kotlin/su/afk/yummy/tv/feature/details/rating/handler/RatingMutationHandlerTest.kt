package su.afk.yummy.tv.feature.details.rating.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.usecase.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeUserRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeRatingUseCase
import su.afk.yummy.tv.domain.library.usecase.SetLibraryUserRatingUseCase
import java.io.IOException

class RatingMutationHandlerTest : BaseUnitTest() {

    private val getRatingSummary: GetAnimeRatingSummaryUseCase = mockk()
    private val getListStats: GetAnimeListStatsUseCase = mockk()
    private val getUserRating: GetAnimeUserRatingUseCase = mockk()
    private val setRating: SetAnimeRatingUseCase = mockk(relaxed = true)
    private val deleteRating: DeleteAnimeRatingUseCase = mockk(relaxed = true)
    private val pendingMutationQueue: PendingMutationQueue = mockk()
    private val setLibraryUserRating: SetLibraryUserRatingUseCase = mockk(relaxed = true)

    @Test
    fun `parts of the rating screen load independently`() = runTest {
        val summary: AnimeRatingSummary = mockk()
        coEvery { getRatingSummary(ANIME_ID) } returns summary
        coEvery { getListStats(ANIME_ID) } throws IllegalStateException("stats")
        coEvery { getUserRating(ANIME_ID) } returns 8

        val result = createHandler().load(ANIME_ID)

        assertEquals(summary, result.ratingSummary.getOrNull())
        assertTrue(result.listStats.isFailure)
        assertEquals(8, result.userRating.getOrNull())
    }

    @Test
    fun `successful mutations succeed`() = runTest {
        val handler = createHandler()

        assertEquals(RatingMutationResult.Success, handler.setRating(ANIME_ID, 9))
        assertEquals(RatingMutationResult.Success, handler.deleteRating(ANIME_ID))
        coVerify { setRating(ANIME_ID, 9) }
        coVerify { deleteRating(ANIME_ID) }
        coVerify { setLibraryUserRating(ANIME_ID, 9) }
        coVerify { setLibraryUserRating(ANIME_ID, null) }
    }

    @Test
    fun `offline mutations are queued and count as success`() = runTest {
        val error = IOException("offline")
        coEvery { setRating(any(), any()) } throws error
        coEvery { deleteRating(any()) } throws error
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), error) } returns true
        val handler = createHandler()

        assertEquals(RatingMutationResult.Success, handler.setRating(ANIME_ID, 9))
        assertEquals(RatingMutationResult.Success, handler.deleteRating(ANIME_ID))
        coVerify { pendingMutationQueue.enqueueOnNetworkFailure(PendingMutation.SetRating(ANIME_ID, 9), error) }
        coVerify { pendingMutationQueue.enqueueOnNetworkFailure(PendingMutation.DeleteRating(ANIME_ID), error) }
    }

    @Test
    fun `rejected mutation fails`() = runTest {
        val error = IllegalStateException("400")
        coEvery { setRating(any(), any()) } throws error
        coEvery { pendingMutationQueue.enqueueOnNetworkFailure(any(), error) } returns false

        assertEquals(RatingMutationResult.Failure, createHandler().setRating(ANIME_ID, 9))
        coVerify(exactly = 0) { setLibraryUserRating(any(), any()) }
    }

    @Test
    fun `failed summary refresh gives nothing`() = runTest {
        coEvery { getRatingSummary(ANIME_ID) } throws IllegalStateException("network")

        assertNull(createHandler().refreshSummary(ANIME_ID))
    }

    private fun createHandler() = RatingMutationHandler(
        getAnimeRatingSummary = getRatingSummary,
        getAnimeListStats = getListStats,
        getAnimeUserRating = getUserRating,
        setAnimeRating = setRating,
        deleteAnimeRating = deleteRating,
        pendingMutationQueue = pendingMutationQueue,
        setLibraryUserRating = setLibraryUserRating,
    )

    private companion object {
        const val ANIME_ID = 42
    }
}

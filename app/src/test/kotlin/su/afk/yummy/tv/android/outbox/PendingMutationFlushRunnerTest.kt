package su.afk.yummy.tv.android.outbox

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.storage.outbox.MarkWatchedPayload
import su.afk.yummy.tv.core.storage.outbox.PendingMutationEntry
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.usecase.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideosUseCase
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeRatingUseCase
import su.afk.yummy.tv.domain.reviews.usecase.VoteReviewUseCase
import java.io.IOException

class PendingMutationFlushRunnerTest : BaseUnitTest() {

    private val outbox: PendingMutationOutbox = mockk(relaxed = true)
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase = mockk()
    private val removeAnimeList: RemoveAnimeListUseCase = mockk()

    private fun runner() = PendingMutationFlushRunner(
        outbox = outbox,
        saveVideoWatchProgress = saveVideoWatchProgress,
        removeWatchedVideos = mockk<RemoveWatchedVideosUseCase>(),
        setAnimeList = mockk<SetAnimeListUseCase>(),
        removeAnimeList = removeAnimeList,
        setAnimeFavorite = mockk<SetAnimeFavoriteUseCase>(),
        setAnimeRating = mockk<SetAnimeRatingUseCase>(),
        deleteAnimeRating = mockk<DeleteAnimeRatingUseCase>(),
        voteReview = mockk<VoteReviewUseCase>(),
    )

    private fun removeList(id: Long, attempts: Int = 0) =
        PendingMutationEntry(id, PendingMutationTypes.REMOVE_LIST, "1", attemptCount = attempts)

    private fun responseError(code: Int): ResponseException {
        val response = mockk<HttpResponse> { every { status } returns HttpStatusCode.fromValue(code) }
        return ResponseException(response, "body")
    }

    @Test
    fun `доставленная запись удаляется, очередь пуста`() = runTest {
        coEvery { outbox.pending() } returns listOf(removeList(1))
        coEvery { removeAnimeList(1) } returns Unit

        assertTrue(runner().flush())

        coVerify { outbox.remove(1) }
    }

    @Test
    fun `401 не стирает запись, а считается попыткой`() = runTest {
        coEvery { outbox.pending() } returns listOf(removeList(1))
        coEvery { removeAnimeList(1) } throws responseError(401)

        assertFalse(runner().flush())

        coVerify(exactly = 0) { outbox.remove(any()) }
        coVerify { outbox.recordAttemptFailure(1) }
    }

    @Test
    fun `постоянный отказ 404 удаляет запись и не останавливает очередь`() = runTest {
        coEvery { outbox.pending() } returns listOf(removeList(1), removeList(2))
        coEvery { removeAnimeList(1) } throws responseError(404) andThen Unit

        assertTrue(runner().flush())

        coVerify { outbox.remove(1) }
        coVerify { outbox.remove(2) }
    }

    @Test
    fun `без сети попытки не тратятся и порядок сохраняется`() = runTest {
        coEvery { outbox.pending() } returns listOf(removeList(1), removeList(2))
        coEvery { removeAnimeList(any()) } throws IOException("offline")

        assertFalse(runner().flush())

        coVerify(exactly = 0) { outbox.recordAttemptFailure(any()) }
        coVerify(exactly = 0) { outbox.remove(any()) }
        coVerify(exactly = 1) { removeAnimeList(any()) }
    }

    @Test
    fun `5xx после лимита попыток убирает запись`() = runTest {
        coEvery { outbox.pending() } returns listOf(removeList(1, attempts = 7))
        coEvery { removeAnimeList(1) } throws responseError(503)

        assertTrue(runner().flush())

        coVerify { outbox.remove(1) }
    }

    @Test
    fun `секунды просмотра уходят вместе с позицией`() = runTest {
        val payload = MarkWatchedPayload(videoId = 9, timeSeconds = 120, durationSeconds = 1_400, times = listOf(1, 2, 3, 50))
        coEvery { outbox.pending() } returns listOf(
            PendingMutationEntry(1, PendingMutationTypes.MARK_WATCHED, payload.encode()),
        )
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } returns true

        assertTrue(runner().flush())

        coVerify { saveVideoWatchProgress(9, 120, 1_400, listOf(1, 2, 3, 50)) }
    }
}

package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveContinueTargetUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.SuppressContinueWatchingDisplayUseCase
import su.afk.yummy.tv.feature.player.model.PlayerProgressSnapshot
import java.io.IOException

class PlayerProgressHandlerTest : BaseUnitTest() {

    private val saveWatchProgress: SaveWatchProgressUseCase = mockk(relaxed = true)
    private val saveContinueTarget: SaveContinueTargetUseCase = mockk(relaxed = true)
    private val suppressContinueWatching: SuppressContinueWatchingDisplayUseCase = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk()
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase = mockk()
    private val pendingMutationQueue: PendingMutationQueue = mockk(relaxed = true)

    private val context = PlayerProgressContext(animeId = ANIME_ID, animeTitle = "Title", posterUrl = "poster")
    private val sentTimes = mutableListOf<List<Int>>()

    @Before
    fun setUp() {
        every { settingsStore.yaniUserId } returns flowOf(USER_ID)
        every { settingsStore.suggestNextEpisodeOnWatched } returns flowOf(true)
        coEvery { saveVideoWatchProgress(any(), any(), any(), capture(sentTimes)) } returns true
    }

    @Test
    fun `progress without a duration is not saved`() = runTest {
        createHandler().saveProgress(context, snapshot(positionMs = 60_000L, durationMs = 0L))

        coVerify(exactly = 0) { saveWatchProgress(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { saveVideoWatchProgress(any(), any(), any(), any()) }
    }

    @Test
    fun `local progress is saved with the screen metadata`() = runTest {
        createHandler().saveProgress(context, snapshot(positionMs = 60_000L))

        coVerify {
            saveWatchProgress(
                animeId = ANIME_ID,
                episode = "3",
                videoId = VIDEO_ID,
                episodeUrl = "url",
                positionMs = 60_000L,
                durationMs = DURATION_MS,
                animeTitle = "Title",
                posterUrl = "poster",
                playerName = "Kodik",
                dubbing = "AniLibria",
                screenshotUrl = "shot",
            )
        }
    }

    @Test
    fun `watched progress is stored as fully watched`() = runTest {
        createHandler().saveProgress(context, snapshot(positionMs = DURATION_MS - 60_000L))

        coVerify {
            saveWatchProgress(any(), any(), any(), any(), positionMs = DURATION_MS, durationMs = DURATION_MS, any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `server gets the played seconds once and only the new ones later`() = runTest {
        val handler = createHandler()
        (100..102).forEach { handler.recordWatchedSecond(VIDEO_ID, it * 1000L, DURATION_MS) }
        handler.saveProgress(context, snapshot(positionMs = 102_000L))

        (102..104).forEach { handler.recordWatchedSecond(VIDEO_ID, it * 1000L, DURATION_MS) }
        handler.saveProgress(context, snapshot(positionMs = 104_000L), forceRemoteSync = true)

        assertEquals(listOf(listOf(100, 101, 102), listOf(103, 104)), sentTimes)
    }

    @Test
    fun `seconds of a failed sync are sent again`() = runTest {
        coEvery { saveVideoWatchProgress(any(), any(), any(), capture(sentTimes)) } returnsMany listOf(false, true)
        val handler = createHandler()
        handler.recordWatchedSecond(VIDEO_ID, 100_000L, DURATION_MS)
        handler.saveProgress(context, snapshot(positionMs = 100_000L))

        handler.saveProgress(context, snapshot(positionMs = 100_000L), forceRemoteSync = true)

        assertEquals(listOf(listOf(100), listOf(100)), sentTimes)
    }

    @Test
    fun `planned syncs are throttled, a forced one is not`() = runTest {
        val handler = createHandler()

        handler.saveProgress(context, snapshot(positionMs = 60_000L))
        handler.saveProgress(context, snapshot(positionMs = 61_000L))
        coVerify(exactly = 1) { saveVideoWatchProgress(any(), any(), any(), any()) }

        handler.saveProgress(context, snapshot(positionMs = 62_000L), forceRemoteSync = true)
        coVerify(exactly = 2) { saveVideoWatchProgress(any(), any(), any(), any()) }
    }

    @Test
    fun `completion is sent right away and only once`() = runTest {
        val handler = createHandler()
        handler.saveProgress(context, snapshot(positionMs = 60_000L))

        handler.saveProgress(context, snapshot(positionMs = DURATION_MS - 60_000L))
        handler.saveProgress(context, snapshot(positionMs = DURATION_MS - 30_000L), forceRemoteSync = true)

        coVerify(exactly = 2) { saveVideoWatchProgress(any(), any(), any(), any()) }
        // Позиция на сервер — без хвоста в 10 секунд.
        coVerify { saveVideoWatchProgress(VIDEO_ID, DURATION_S - 10, DURATION_S, any()) }
    }

    @Test
    fun `forced sync without network is queued for later`() = runTest {
        val error = IOException("offline")
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } throws error

        createHandler().saveProgress(context, snapshot(positionMs = 60_000L), forceRemoteSync = true)

        coVerify {
            pendingMutationQueue.enqueueOnNetworkFailure(
                PendingMutation.MarkWatched(videoId = VIDEO_ID, timeSeconds = 60, durationSeconds = DURATION_S),
                error,
            )
        }
    }

    @Test
    fun `planned sync without network is not queued`() = runTest {
        coEvery { saveVideoWatchProgress(any(), any(), any(), any()) } throws IOException("offline")

        createHandler().saveProgress(context, snapshot(positionMs = 60_000L))

        coVerify(exactly = 0) { pendingMutationQueue.enqueueOnNetworkFailure(any(), any()) }
    }

    @Test
    fun `guests, short progress and unknown videos are not synced`() = runTest {
        val handler = createHandler()

        handler.saveProgress(context, snapshot(positionMs = 10_000L), forceRemoteSync = true)
        handler.saveProgress(context, snapshot(positionMs = 60_000L, videoId = 0), forceRemoteSync = true)
        every { settingsStore.yaniUserId } returns flowOf(0)
        handler.saveProgress(context, snapshot(positionMs = 60_000L), forceRemoteSync = true)

        coVerify(exactly = 0) { saveVideoWatchProgress(any(), any(), any(), any()) }
    }

    @Test
    fun `played seconds near the end are clamped to the tolerance`() = runTest {
        val handler = createHandler()
        handler.recordWatchedSecond(VIDEO_ID, DURATION_MS - 1_000L, DURATION_MS)
        handler.recordWatchedSecond(0, 50_000L, DURATION_MS)

        handler.saveProgress(context, snapshot(positionMs = 60_000L))

        assertEquals(listOf(listOf(DURATION_S - 10)), sentTimes)
    }

    @Test
    fun `continue target and suppression use the screen context`() = runTest {
        val handler = createHandler()
        val snapshot = snapshot(positionMs = 0L)

        handler.saveContinueTarget(context, snapshot)
        handler.suppressContinueWatchingDisplay(context)

        coVerify {
            saveContinueTarget(ANIME_ID, "3", VIDEO_ID, "url", "Title", "poster", "Kodik", "AniLibria", "shot")
        }
        coVerify { suppressContinueWatching(ANIME_ID) }
    }

    private fun snapshot(positionMs: Long, durationMs: Long = DURATION_MS, videoId: Int = VIDEO_ID) =
        PlayerProgressSnapshot(
            episode = "3",
            episodeUrl = "url",
            videoId = videoId,
            playerName = "Kodik",
            dubbing = "AniLibria",
            screenshotUrl = "shot",
            positionMs = positionMs,
            durationMs = durationMs,
        )

    private fun createHandler() = PlayerProgressHandler(
        saveWatchProgress = saveWatchProgress,
        saveContinueTargetUseCase = saveContinueTarget,
        suppressContinueWatchingDisplayUseCase = suppressContinueWatching,
        settingsStore = settingsStore,
        saveVideoWatchProgress = saveVideoWatchProgress,
        pendingMutationQueue = pendingMutationQueue,
    )

    private companion object {
        const val ANIME_ID = 42
        const val VIDEO_ID = 7
        const val USER_ID = 5
        const val DURATION_S = 1440
        const val DURATION_MS = DURATION_S * 1000L
    }
}

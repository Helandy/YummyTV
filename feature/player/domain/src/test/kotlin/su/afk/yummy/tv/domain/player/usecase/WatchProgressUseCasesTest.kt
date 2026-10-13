package su.afk.yummy.tv.domain.player.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository

class WatchProgressUseCasesTest : BaseUnitTest() {

    private val repository: WatchProgressRepository = mockk(relaxed = true)
    private val clock: AppClock = mockk()

    @Before
    fun setUp() {
        every { clock.nowMillis() } returns NOW
        coEvery { repository.get(any(), any()) } returns null
    }

    @Test
    fun `progress gets the current time`() = runTest {
        saveProgress()

        coVerify { repository.save(ANIME_ID, "3", 7, "url", 60_000L, 1_440_000L, NOW, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `activity time never goes back for the same episode`() = runTest {
        // Прошлая запись серии «из будущего» (та же миллисекунда, сдвиг часов) — порядок не ломаем.
        coEvery { repository.get(ANIME_ID, "3") } returns progress(updatedAt = NOW + 500L)

        saveProgress()
        SaveContinueTargetUseCase(repository, clock)(ANIME_ID, "3", 7, "url", "Title", "poster", "Kodik", "AniLibria", "shot")

        coVerify { repository.save(any(), any(), any(), any(), any(), any(), NOW + 501L, any(), any(), any(), any(), any()) }
        coVerify { repository.saveContinueTarget(any(), any(), any(), any(), NOW + 501L, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `local play without a title is not looked up`() = runTest {
        SaveWatchProgressUseCase(repository, clock)(0, "3", 0, "file://x", 1L, 2L, "", "", "", "", "")

        coVerify(exactly = 0) { repository.get(any(), any()) }
    }

    @Test
    fun `marking watched stores the full duration as the position`() = runTest {
        MarkEpisodeWatchedLocallyUseCase(repository, clock)(ANIME_ID, "3", 7, "url", 1_440_000L, "Title", "poster", "Kodik", "AniLibria", "shot")

        coVerify { repository.save(ANIME_ID, "3", 7, "url", 1_440_000L, 1_440_000L, NOW, "Title", "poster", "Kodik", "AniLibria", "shot") }
    }

    @Test
    fun `marking watched needs a title, an episode and a duration`() = runTest {
        val useCase = MarkEpisodeWatchedLocallyUseCase(repository, clock)

        useCase(0, "3", 7, "url", 1_440_000L, "", "", "", "", "")
        useCase(ANIME_ID, " ", 7, "url", 1_440_000L, "", "", "", "", "")
        useCase(ANIME_ID, "3", 7, "url", 0L, "", "", "", "", "")

        coVerify(exactly = 0) { repository.save(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    private suspend fun saveProgress() =
        SaveWatchProgressUseCase(repository, clock)(ANIME_ID, "3", 7, "url", 60_000L, 1_440_000L, "Title", "poster", "Kodik", "AniLibria", "shot")

    private fun progress(updatedAt: Long) = AnimeWatchProgress(
        animeId = ANIME_ID,
        episode = "3",
        episodeUrl = "url",
        positionMs = 1L,
        durationMs = 2L,
        updatedAt = updatedAt,
    )

    private companion object {
        const val ANIME_ID = 42
        const val NOW = 1_000_000L
    }
}

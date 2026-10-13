package su.afk.yummy.tv.domain.watching.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.watching.ContinueWatchingRemoteProgressSwitch
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.MigrateContinueWatchingProgressUseCase

class ContinueWatchingLaunchUseCasesTest : BaseUnitTest() {

    private val getAnimeVideos: GetAnimeVideosUseCase = mockk()
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase = mockk()
    private val migrateProgress: MigrateContinueWatchingProgressUseCase = mockk(relaxed = true)

    private val ep2 = video(id = 2, episode = "2")
    private val ep3 = video(id = 3, episode = "3")

    // ---------------- loading ----------------

    @Test
    fun `refresh falls back to the cache, a missing cache to nothing`() = runTest {
        val load = LoadContinueWatchingVideosUseCase(getAnimeVideos, refreshAnimeVideos)
        coEvery { refreshAnimeVideos(ANIME_ID) } throws IllegalStateException("network")
        coEvery { getAnimeVideos(ANIME_ID) } returns listOf(ep2)

        assertEquals(listOf(ep2), load(ANIME_ID, refresh = true))

        coEvery { getAnimeVideos(ANIME_ID) } throws IllegalStateException("db")
        assertTrue(load(ANIME_ID, refresh = false).isEmpty())
        assertTrue(load(0, refresh = true).isEmpty())
    }

    // ---------------- resolving ----------------

    @Test
    fun `local progress resumes its own video`() = runTest {
        coEvery { getAnimeVideos(ANIME_ID) } returns listOf(ep2, ep3)

        val launch = resolve(entry(videoId = 2, episode = "2", positionMs = 300_000L), refresh = false)

        assertEquals(2, launch.video.id)
        assertEquals(300_000L, launch.resumeFromMs)
        assertNull(launch.remoteProgressSwitch)
    }

    @Test
    fun `newer progress from another device wins and is announced`() = runTest {
        val watchedOnTv = ep3.copy(watchedEndTimeSeconds = 400, watchedDateSeconds = LOCAL_UPDATED_AT / 1000 + 60)
        coEvery { refreshAnimeVideos(ANIME_ID) } returns listOf(ep2, watchedOnTv)

        val launch = resolve(entry(videoId = 2, episode = "2", positionMs = 300_000L), refresh = true)

        assertEquals(3, launch.video.id)
        assertEquals(400_000L, launch.resumeFromMs)
        assertEquals(ContinueWatchingRemoteProgressSwitch("3", 400_000L), launch.remoteProgressSwitch)
    }

    @Test
    fun `older server progress is ignored`() = runTest {
        val stale = ep3.copy(watchedEndTimeSeconds = 400, watchedDateSeconds = LOCAL_UPDATED_AT / 1000 - 60)
        coEvery { refreshAnimeVideos(ANIME_ID) } returns listOf(ep2, stale)

        val launch = resolve(entry(videoId = 2, episode = "2", positionMs = 300_000L), refresh = true)

        assertEquals(2, launch.video.id)
        assertEquals(300_000L, launch.resumeFromMs)
    }

    @Test
    fun `entry with a placeholder episode is migrated to the matched one`() = runTest {
        coEvery { getAnimeVideos(ANIME_ID) } returns listOf(ep2, ep3)

        val launch = resolve(entry(videoId = 3, episode = "", positionMs = 120_000L), refresh = false)

        assertEquals(3, launch.video.id)
        coVerify { migrateProgress(any()) }
    }

    private suspend fun resolve(entry: HomeContinueWatchingItem, refresh: Boolean) =
        ResolveContinueWatchingLaunchUseCase(
            LoadContinueWatchingVideosUseCase(getAnimeVideos, refreshAnimeVideos),
            migrateProgress,
        )(entry, refreshProgressOnLaunch = refresh)

    private fun entry(videoId: Int, episode: String, positionMs: Long) = HomeContinueWatchingItem(
        animeId = ANIME_ID,
        animeTitle = "Title",
        description = "",
        poster = null,
        videoId = videoId,
        episode = episode,
        episodeUrl = "https://kodik.info/seria/$videoId",
        positionMs = positionMs,
        durationMs = 1_440_000L,
        updatedAt = LOCAL_UPDATED_AT,
        playerName = "Kodik",
        dubbing = "AniLibria",
        screenshotUrl = "",
    )

    private fun video(id: Int, episode: String) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = "AniLibria",
        player = "Kodik",
        playerId = null,
        iframeUrl = "https://kodik.info/seria/$id",
        durationSeconds = 1_440,
    )

    private companion object {
        const val ANIME_ID = 42
        const val LOCAL_UPDATED_AT = 1_700_000_000_000L
    }
}

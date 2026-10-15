package su.afk.yummy.tv.domain.watching.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.usecase.FetchAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.model.ContinueWatchingSource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.usecase.RemoveCachedContinueWatchingUseCase

class ResolveNextEpisodeLaunchUseCaseTest : BaseUnitTest() {

    private val fetchAnimeVideos: FetchAnimeVideosUseCase = mockk()
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase = mockk()
    private val removeCachedContinueWatching: RemoveCachedContinueWatchingUseCase = mockk(relaxed = true)

    private val ep2Watched = video(id = 2, episode = "2", dubbing = "LostFilm", player = "Zedfilm")
        .copy(watchedEndTimeSeconds = 1_430, watchedDateSeconds = 1_700_000_000L)
    private val ep3Kodik = video(id = 31, episode = "03", dubbing = "AniLibria", player = "Kodik")
    private val ep3Zedfilm = video(id = 32, episode = "03", dubbing = "LostFilm", player = "Zedfilm")

    @Test
    fun `next episode starts from zero with the dubbing of the last watched video`() = runTest {
        coEvery { fetchAnimeVideos(ANIME_ID) } returns listOf(ep2Watched, ep3Kodik, ep3Zedfilm)

        val launch = createUseCase()(nextEpisodeEntry(episode = "3"))

        assertEquals(32, launch?.video?.id)
        assertEquals("03", launch?.video?.episode)
        assertEquals(0L, launch?.resumeFromMs)
        assertNull(launch?.remoteProgressSwitch)
        coVerify(exactly = 0) { removeCachedContinueWatching(any()) }
    }

    @Test
    fun `next episode without the last dubbing falls back to another video of the same episode`() = runTest {
        coEvery { fetchAnimeVideos(ANIME_ID) } returns listOf(ep2Watched, ep3Kodik)

        val launch = createUseCase()(nextEpisodeEntry(episode = "3"))

        assertEquals(31, launch?.video?.id)
    }

    @Test
    fun `missing episode in a fresh list hides the title`() = runTest {
        coEvery { fetchAnimeVideos(ANIME_ID) } returns listOf(ep2Watched)

        assertNull(createUseCase()(nextEpisodeEntry(episode = "3")))
        coVerify(exactly = 1) { removeCachedContinueWatching(ANIME_ID) }
    }

    @Test
    fun `empty fresh list does not hide the title`() = runTest {
        coEvery { fetchAnimeVideos(ANIME_ID) } returns emptyList()

        assertNull(createUseCase()(nextEpisodeEntry(episode = "3")))
        coVerify(exactly = 0) { removeCachedContinueWatching(any()) }
    }

    @Test
    fun `offline decision uses the cache and never hides the title`() = runTest {
        coEvery { fetchAnimeVideos(ANIME_ID) } throws IllegalStateException("network")
        coEvery { getCachedAnimeVideos(ANIME_ID) } returns listOf(ep2Watched)

        assertNull(createUseCase()(nextEpisodeEntry(episode = "3")))
        coVerify(exactly = 0) { removeCachedContinueWatching(any()) }

        coEvery { getCachedAnimeVideos(ANIME_ID) } returns listOf(ep2Watched, ep3Zedfilm)
        assertEquals(32, createUseCase()(nextEpisodeEntry(episode = "3"))?.video?.id)

        coEvery { getCachedAnimeVideos(ANIME_ID) } returns null
        assertNull(createUseCase()(nextEpisodeEntry(episode = "3")))
    }

    @Test
    fun `continue watching launch delegates next episode cards`() = runTest {
        coEvery { fetchAnimeVideos(ANIME_ID) } returns listOf(ep2Watched, ep3Zedfilm)
        val loadVideos: LoadContinueWatchingVideosUseCase = mockk()

        val launch = ResolveContinueWatchingLaunchUseCase(
            loadContinueWatchingVideos = loadVideos,
            migrateContinueWatchingProgress = mockk(),
            resolveNextEpisodeLaunch = createUseCase(),
        )(nextEpisodeEntry(episode = "3"), refreshProgressOnLaunch = false)

        assertEquals(32, launch?.video?.id)
        coVerify(exactly = 0) { loadVideos(any(), any()) }
    }

    private fun createUseCase() = ResolveNextEpisodeLaunchUseCase(
        fetchAnimeVideos = fetchAnimeVideos,
        getCachedAnimeVideos = getCachedAnimeVideos,
        removeCachedContinueWatching = removeCachedContinueWatching,
    )

    private fun nextEpisodeEntry(episode: String) = HomeContinueWatchingItem(
        animeId = ANIME_ID,
        animeTitle = "Title",
        description = "",
        poster = null,
        videoId = 0,
        episode = episode,
        episodeUrl = "",
        positionMs = 0L,
        durationMs = 0L,
        updatedAt = 1_700_000_000_000L,
        playerName = "",
        dubbing = "",
        screenshotUrl = "",
        source = ContinueWatchingSource.REMOTE_NEXT_EPISODE,
    )

    private fun video(id: Int, episode: String, dubbing: String, player: String) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = null,
        iframeUrl = "https://kodik.info/seria/$id",
        durationSeconds = 1_440,
    )

    private companion object {
        const val ANIME_ID = 42
    }
}

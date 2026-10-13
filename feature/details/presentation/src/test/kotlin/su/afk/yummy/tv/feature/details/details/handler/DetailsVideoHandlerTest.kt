package su.afk.yummy.tv.feature.details.details.handler

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

class DetailsVideoHandlerTest : BaseUnitTest() {

    private val getAnimeVideos: GetAnimeVideosUseCase = mockk()
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase = mockk()
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase = mockk()

    private val ep1Popular = video(id = 1, episode = "1", dubbing = "Popular", views = 100)
    private val ep2Popular = video(id = 2, episode = "2", dubbing = "Popular", views = 100)
    private val ep1Rare = video(id = 3, episode = "1", dubbing = "Rare", views = 1)

    @Test
    fun `missing or broken cache gives nothing`() = runTest {
        val handler = createHandler()

        coEvery { getCachedAnimeVideos(ANIME_ID) } returns null
        assertNull(handler.loadCached(ANIME_ID))

        coEvery { getCachedAnimeVideos(ANIME_ID) } throws IllegalStateException("db")
        assertNull(handler.loadCached(ANIME_ID))
    }

    @Test
    fun `loaded and refreshed videos are returned and failures are kept`() = runTest {
        coEvery { getAnimeVideos(ANIME_ID) } returns listOf(ep1Popular)
        coEvery { refreshAnimeVideos(ANIME_ID) } throws IllegalStateException("network")
        val handler = createHandler()

        assertEquals(listOf(ep1Popular), handler.load(ANIME_ID).getOrThrow().videos)
        assertTrue(handler.refresh(ANIME_ID).isFailure)
    }

    @Test
    fun `without progress the first episode of the most watched dubbing is offered`() {
        val target = createHandler().resolveWatchTarget(
            ANIME_ID,
            listOf(ep1Rare, ep2Popular, ep1Popular),
            DetailsWatchProgressIndex(),
        )

        assertEquals(DetailsWatchTarget.Initial(ep1Popular), target)
    }

    @Test
    fun `unfinished episode is continued`() {
        val progress = AnimeWatchProgress(
            animeId = ANIME_ID,
            episode = "2",
            videoId = 2,
            episodeUrl = ep2Popular.iframeUrl,
            positionMs = 300_000L,
            durationMs = 1_440_000L,
            updatedAt = 1L,
            playerName = "Kodik",
            dubbing = "Popular",
        )

        val target = createHandler().resolveWatchTarget(
            ANIME_ID,
            listOf(ep1Popular, ep2Popular),
            DetailsWatchProgressIndex(listOf(progress)),
        )

        assertEquals(2, (target as DetailsWatchTarget.Continue).video.id)
    }

    @Test
    fun `no videos means no target`() {
        assertNull(createHandler().resolveWatchTarget(ANIME_ID, emptyList(), DetailsWatchProgressIndex()))
    }

    private fun video(id: Int, episode: String, dubbing: String, views: Int) = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = "Kodik",
        playerId = null,
        iframeUrl = "https://kodik.info/seria/$id",
        durationSeconds = 1_440,
        views = views,
    )

    private fun createHandler() = DetailsVideoHandler(getAnimeVideos, getCachedAnimeVideos, refreshAnimeVideos)

    private companion object {
        const val ANIME_ID = 42
    }
}

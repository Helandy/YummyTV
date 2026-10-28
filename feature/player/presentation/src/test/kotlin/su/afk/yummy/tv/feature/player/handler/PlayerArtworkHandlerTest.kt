package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnail
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailResolver
import su.afk.yummy.tv.feature.player.model.PlayerArtworkSource

class PlayerArtworkHandlerTest : BaseUnitTest() {

    private val thumbnailResolver: EpisodeThumbnailResolver = mockk()

    @Test
    fun `episode thumbnail from the resolver becomes the artwork`() = runTest {
        coEvery { thumbnailResolver.resolveUrl(any()) } returns THUMBNAIL

        val artwork = createHandler().resolve(source(episodeUrl = KODIK_URL))

        assertEquals(THUMBNAIL, artwork)
    }

    @Test
    fun `resolver gets the episode with anime id and kodik iframe hint`() = runTest {
        coEvery { thumbnailResolver.resolveUrl(any()) } returns THUMBNAIL

        createHandler().resolve(source(episodeUrl = KODIK_URL).copy(animeId = 7, episode = "3"))

        coVerify { thumbnailResolver.resolveUrl(EpisodeThumbnail(7, "3", iframeUrl = KODIK_URL)) }
    }

    @Test
    fun `direct screenshot is passed as an image hint`() = runTest {
        coEvery { thumbnailResolver.resolveUrl(any()) } returns SCREENSHOT

        val artwork = createHandler().resolve(source(screenshotUrl = SCREENSHOT))

        assertEquals(SCREENSHOT, artwork)
        coVerify { thumbnailResolver.resolveUrl(EpisodeThumbnail(imageUrl = SCREENSHOT)) }
    }

    @Test
    fun `unresolved thumbnail falls back to the poster`() = runTest {
        coEvery { thumbnailResolver.resolveUrl(any()) } returns null

        assertEquals(POSTER, createHandler().resolve(source(episodeUrl = KODIK_URL)))
    }

    @Test
    fun `nothing to resolve gives the poster and no poster gives nothing`() = runTest {
        val handler = createHandler()

        assertEquals(POSTER, handler.resolve(source()))
        assertNull(handler.resolve(source(posterUrl = "")))
        coVerify(exactly = 0) { thumbnailResolver.resolveUrl(any()) }
    }

    private fun source(
        screenshotUrl: String = "",
        episodeUrl: String = "https://cdn.example/episode",
        posterUrl: String = POSTER,
    ) = PlayerArtworkSource(screenshotUrl = screenshotUrl, episodeUrl = episodeUrl, posterUrl = posterUrl)

    private fun createHandler() = PlayerArtworkHandler(thumbnailResolver)

    private companion object {
        const val KODIK_URL = "https://kodik.info/seria/1/hash/720p"
        const val THUMBNAIL = "https://kodik.info/thumb.jpg"
        const val SCREENSHOT = "https://img.example/shot.webp"
        const val POSTER = "https://img.example/poster.jpg"
    }
}

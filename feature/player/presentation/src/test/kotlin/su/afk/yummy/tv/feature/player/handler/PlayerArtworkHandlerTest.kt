package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.EpisodePreviewProvider
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.feature.player.model.PlayerArtworkSource

class PlayerArtworkHandlerTest : BaseUnitTest() {

    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase = mockk()
    private val episodePreviews: EpisodePreviewProvider = mockk {
        coEvery { previewUrl(any(), any()) } returns null
    }

    @Test
    fun `kodik episode gets its thumbnail`() = runTest {
        coEvery { resolveKodikThumbnailUrl(KODIK_URL) } returns THUMBNAIL

        val artwork = createHandler().resolve(source(episodeUrl = KODIK_URL))

        assertEquals(THUMBNAIL, artwork)
    }

    @Test
    fun `unresolved kodik thumbnail falls back to the poster`() = runTest {
        coEvery { resolveKodikThumbnailUrl(KODIK_URL) } returns null

        val artwork = createHandler().resolve(source(episodeUrl = KODIK_URL))

        assertEquals(POSTER, artwork)
    }

    @Test
    fun `direct screenshot is used without a kodik request`() = runTest {
        val artwork = createHandler().resolve(source(screenshotUrl = SCREENSHOT))

        assertEquals(SCREENSHOT, artwork)
        coVerify(exactly = 0) { resolveKodikThumbnailUrl(any()) }
    }

    @Test
    fun `no screenshot gives the poster and no poster gives nothing`() = runTest {
        val handler = createHandler()

        assertEquals(POSTER, handler.resolve(source()))
        assertNull(handler.resolve(source(posterUrl = "")))
    }

    @Test
    fun `tenrai preview wins over kodik`() = runTest {
        coEvery { episodePreviews.previewUrl(7, "3") } returns TENRAI

        val artwork = createHandler().resolve(source(episodeUrl = KODIK_URL).copy(animeId = 7, episode = "3"))

        assertEquals(TENRAI, artwork)
        coVerify(exactly = 0) { resolveKodikThumbnailUrl(any()) }
    }

    private fun source(
        screenshotUrl: String = "",
        episodeUrl: String = "https://cdn.example/episode",
        posterUrl: String = POSTER,
    ) = PlayerArtworkSource(screenshotUrl = screenshotUrl, episodeUrl = episodeUrl, posterUrl = posterUrl)

    private fun createHandler() = PlayerArtworkHandler(resolveKodikThumbnailUrl, episodePreviews)

    private companion object {
        const val KODIK_URL = "https://kodik.info/seria/1/hash/720p"
        const val THUMBNAIL = "https://kodik.info/thumb.jpg"
        const val TENRAI = "https://img.example/tenrai.jpg"
        const val SCREENSHOT = "https://img.example/shot.webp"
        const val POSTER = "https://img.example/poster.jpg"
    }
}

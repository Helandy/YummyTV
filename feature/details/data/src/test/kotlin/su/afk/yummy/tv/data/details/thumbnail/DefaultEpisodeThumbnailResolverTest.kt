package su.afk.yummy.tv.data.details.thumbnail

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnail
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase

class DefaultEpisodeThumbnailResolverTest : BaseUnitTest() {

    private val tenrai: TenraiEpisodeThumbnails = mockk()
    private val cachedVideos: GetCachedAnimeVideosUseCase = mockk()
    private val kodik: ResolveKodikThumbnailUrlUseCase = mockk()

    private fun resolver() = DefaultEpisodeThumbnailResolver(tenrai, cachedVideos, kodik)

    @Test
    fun `tenrai wins over everything else`() = runTest {
        coEvery { tenrai.previewUrl(ANIME_ID, "1") } returns TENRAI

        val url = resolver().resolveUrl(EpisodeThumbnail(ANIME_ID, "1", IFRAME, IMAGE))

        assertEquals(TENRAI, url)
        coVerify(exactly = 0) { kodik(any()) }
    }

    @Test
    fun `direct image is used before any kodik request`() = runTest {
        coEvery { tenrai.previewUrl(ANIME_ID, "1") } returns null

        val url = resolver().resolveUrl(EpisodeThumbnail(ANIME_ID, "1", IFRAME, IMAGE))

        assertEquals(IMAGE, url)
        coVerify(exactly = 0) { kodik(any()) }
    }

    @Test
    fun `kodik is the fallback for the screen iframe`() = runTest {
        coEvery { tenrai.previewUrl(ANIME_ID, "1") } returns null
        coEvery { kodik(IFRAME) } returns KODIK_THUMB

        assertEquals(KODIK_THUMB, resolver().resolveUrl(EpisodeThumbnail(ANIME_ID, "1", IFRAME)))
    }

    @Test
    fun `without iframe the kodik video of the episode comes from cached videos`() = runTest {
        coEvery { tenrai.previewUrl(ANIME_ID, "01") } returns null
        coEvery { cachedVideos(ANIME_ID) } returns listOf(
            video(episode = "2", iframe = "//kodikplayer.com/other"),
            video(episode = "1", iframe = IFRAME),
        )
        coEvery { kodik(IFRAME) } returns KODIK_THUMB

        assertEquals(KODIK_THUMB, resolver().resolveUrl(EpisodeThumbnail(ANIME_ID, "01")))
    }

    @Test
    fun `nothing known gives null without network`() = runTest {
        assertNull(resolver().resolveUrl(EpisodeThumbnail()))
        coVerify(exactly = 0) { tenrai.previewUrl(any(), any()) }
    }

    private fun video(episode: String, iframe: String) = mockk<AnimeVideo>(relaxed = true) {
        coEvery { this@mockk.episode } returns episode
        coEvery { iframeUrl } returns iframe
        coEvery { player } returns "Плеер Kodik"
    }

    private companion object {
        const val ANIME_ID = 5
        const val TENRAI = "https://cdn.example/tenrai.jpg"
        const val IMAGE = "https://i.kodikres.com/screenshots/1.jpg"
        const val IFRAME = "//kodikplayer.com/season/1/a/720p?episode=1"
        const val KODIK_THUMB = "https://kodik.example/thumb.jpg"
    }
}

package su.afk.yummy.tv.source.studiomir.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.studiomir.data.network.StudioMirApi
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget
import java.io.IOException

class StudioMirRepositoryImplTest : BaseUnitTest() {

    private val api: StudioMirApi = mockk()
    private val pageUrl = "https://studiomir.club/anime/107-hotaru-no-yomeiri"

    private fun page() = requireNotNull(javaClass.classLoader?.getResourceAsStream("studiomir-page.html")).readBytes().decodeToString()

    private fun createRepository() = StudioMirRepositoryImpl(api)

    @Test
    fun `episode streams come from the page playlist and the best quality is the default`() = runTest {
        coEvery { api.getText(pageUrl) } returns page()

        val streams = requireNotNull(createRepository().getStreams(StudioMirTarget("107-hotaru-no-yomeiri", 2)))

        assertEquals(listOf("720p", "1080p"), streams.qualities.keys.toList())
        assertEquals(streams.qualities.getValue("1080p"), streams.masterUrl)
        assertEquals(true, streams.masterUrl.contains("/encoded/107/2/hls/1080/"))
    }

    @Test
    fun `missing page or episode gives null`() = runTest {
        coEvery { api.getText(pageUrl) } returns null
        assertNull(createRepository().getStreams(StudioMirTarget("107-hotaru-no-yomeiri", 1)))

        coEvery { api.getText(pageUrl) } returns page()
        assertNull(createRepository().getStreams(StudioMirTarget("107-hotaru-no-yomeiri", 9)))
    }

    @Test
    fun `network error propagates`() = runTest {
        coEvery { api.getText(any()) } throws IOException("down")

        val error = runCatching { createRepository().getStreams(StudioMirTarget("107-x", 1)) }.exceptionOrNull()

        assertTrue(error is IOException)
    }
}

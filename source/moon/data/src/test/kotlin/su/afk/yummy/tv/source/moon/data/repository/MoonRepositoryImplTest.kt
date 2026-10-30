package su.afk.yummy.tv.source.moon.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.moon.data.network.MoonApi
import su.afk.yummy.tv.source.moon.data.utils.MoonTestPage
import su.afk.yummy.tv.source.moon.domain.model.MoonStreamsResult

class MoonRepositoryImplTest : BaseUnitTest() {

    private val api: MoonApi = mockk()
    private val repository = MoonRepositoryImpl(api)

    @Test
    fun `page gives the stream with player headers, the query of the link is dropped`() = runTest {
        coEvery { api.getPage("https://moonanime.art/iframe/abcd1234/") } returns MoonTestPage.page("https://cdn.example/v/index.m3u8")

        val result = repository.getStreams("https://moonanime.art/iframe/abcd1234/?player=animeon.club&api=1") as MoonStreamsResult.Success

        assertEquals("https://cdn.example/v/index.m3u8", result.streams.url)
        assertEquals("https://moonanime.art/iframe/abcd1234/", result.streams.headers["Referer"])
        assertEquals("https://moonanime.art", result.streams.headers["Origin"])
        assertEquals(null, result.streams.qualities)
    }

    @Test
    fun `mirror page is read from its own host and the origin follows it`() = runTest {
        coEvery { api.getPage("https://moonanime.net/iframe/abcd1234/") } returns MoonTestPage.page("https://cdn.example/v/index.m3u8")

        val result = repository.getStreams("https://moonanime.net/iframe/abcd1234/?player=animeon.club&api=1") as MoonStreamsResult.Success

        assertEquals("https://moonanime.net/iframe/abcd1234/", result.streams.headers["Referer"])
        assertEquals("https://moonanime.net", result.streams.headers["Origin"])
    }

    @Test
    fun `missing page is not found, changed page and foreign link fail`() = runTest {
        coEvery { api.getPage(any()) } returns null
        assertEquals(MoonStreamsResult.NotFound, repository.getStreams("https://moonanime.art/iframe/abcd1234/"))

        coEvery { api.getPage(any()) } returns "<html></html>"
        assertTrue(repository.getStreams("https://moonanime.art/iframe/abcd1234/") is MoonStreamsResult.Failed)
        assertTrue(repository.getStreams("https://evil.example/iframe/abcd1234/") is MoonStreamsResult.Failed)
    }
}

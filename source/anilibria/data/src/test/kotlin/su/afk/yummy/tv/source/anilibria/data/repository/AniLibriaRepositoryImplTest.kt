package su.afk.yummy.tv.source.anilibria.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaEpisodeDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi

class AniLibriaRepositoryImplTest : BaseUnitTest() {

    private val api: AniLibriaApi = mockk()

    private fun createRepository() = AniLibriaRepositoryImpl(api)

    @Test
    fun `stream links are requested from AniLibria only by getRelease`() = runTest {
        coEvery { api.getRelease(9542) } returns AniLibriaReleaseDto(
            id = 9542,
            episodes = listOf(AniLibriaEpisodeDto(ordinal = 1.0, hls_720 = "https://cache.libria.fun/a.m3u8")),
        )

        val release = createRepository().getRelease(9542)

        assertEquals("https://cache.libria.fun/a.m3u8", release?.episodes?.single()?.hls720)
    }
}

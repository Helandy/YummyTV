package su.afk.yummy.tv.source.torrent.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection
import su.afk.yummy.tv.source.torrent.domain.model.TorrentStream
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository

class BuildTorrentStreamUseCaseTest : BaseUnitTest() {

    private val torrServer: TorrServerRepository = mockk()
    private val file = TorrentFile(3, "Show/ep03.mkv", 1)
    private val selection = TorrentSelection("hash", "Show", file)
    private val stream = TorrentStream("http://srv/stream/ep03.mkv", emptyMap())

    private fun createUseCase() = BuildTorrentStreamUseCase(torrServer)

    @Test
    fun `stream is returned once server starts sending data`() = runTest {
        coEvery { torrServer.streamFor("hash", file) } returns stream
        coEvery { torrServer.awaitStreamData(stream) } returns true

        assertEquals(stream, createUseCase()(selection))
    }

    @Test
    fun `null is returned when data never arrives`() = runTest {
        coEvery { torrServer.streamFor("hash", file) } returns stream
        coEvery { torrServer.awaitStreamData(stream) } returns false

        assertNull(createUseCase()(selection))
        coVerify(exactly = 1) { torrServer.awaitStreamData(stream) }
    }
}

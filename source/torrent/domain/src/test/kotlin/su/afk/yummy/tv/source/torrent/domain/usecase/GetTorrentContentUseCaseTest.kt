package su.afk.yummy.tv.source.torrent.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository

class GetTorrentContentUseCaseTest : BaseUnitTest() {

    private val torrServer: TorrServerRepository = mockk()

    private fun file(index: Int, path: String) = TorrentFile(index, path, 1)

    @Test
    fun `non video files are dropped and episode file goes first`() = runTest {
        coEvery { torrServer.addTorrent("magnet", any(), any()) } returns TorrentContent(
            hash = "h",
            title = "Show",
            files = listOf(
                file(1, "Show/Show - 01.mkv"),
                file(2, "Show/Show - 02.mkv"),
                file(3, "Show/Show - 03.mkv"),
                file(4, "Show/subs/Show - 02.ass"),
            ),
        )

        val content = GetTorrentContentUseCase(torrServer)("magnet", "2")

        assertEquals(listOf(2, 1, 3), content.files.map { it.index })
    }
}

package su.afk.yummy.tv.source.torrent.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository

class GetKnownTorrentHashesUseCaseTest : BaseUnitTest() {

    private val torrServer: TorrServerRepository = mockk()
    private val useCase = GetKnownTorrentHashesUseCase(torrServer)

    @Test
    fun `returns hashes reported by server`() = runTest {
        coEvery { torrServer.knownHashes() } returns setOf("a", "b")

        assertEquals(setOf("a", "b"), useCase())
    }
}

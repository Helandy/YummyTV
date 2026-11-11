package su.afk.yummy.tv.source.torrent.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.repository.TorrentIndexerRepository
import java.io.IOException

class SearchTorrentsUseCaseTest : BaseUnitTest() {

    private val indexer: TorrentIndexerRepository = mockk()

    private fun result(link: String, seeders: Int) =
        TorrentResult(title = link, sizeBytes = 1, seeders = seeders, peers = 0, tracker = "t", link = link)

    private fun createUseCase() = SearchTorrentsUseCase(indexer)

    @Test
    fun `results are merged without duplicates and sorted by seeders`() = runTest {
        coEvery { indexer.search("ru") } returns listOf(result("a", 1), result("b", 10))
        coEvery { indexer.search("en") } returns listOf(result("a", 99), result("c", 5))

        val links = createUseCase()(listOf("ru", "en")).map { it.link }

        // дубль "a" берётся из первого запроса, где у него 1 сид
        assertEquals(listOf("b", "c", "a"), links)
    }

    @Test
    fun `blank and repeated queries are skipped`() = runTest {
        coEvery { indexer.search("x") } returns listOf(result("a", 1))

        val results = createUseCase()(listOf(" x ", "", "x"))

        assertEquals(1, results.size)
    }

    @Test
    fun `one failed query is ignored when another succeeds`() = runTest {
        coEvery { indexer.search("ru") } throws IOException("down")
        coEvery { indexer.search("en") } returns listOf(result("a", 1))

        assertEquals(1, createUseCase()(listOf("ru", "en")).size)
    }

    @Test
    fun `all queries failing rethrows`() = runTest {
        coEvery { indexer.search(any()) } throws IOException("down")

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { createUseCase()(listOf("ru", "en")) }
        }
    }
}

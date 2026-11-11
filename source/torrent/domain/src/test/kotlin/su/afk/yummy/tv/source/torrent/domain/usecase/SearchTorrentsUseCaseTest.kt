package su.afk.yummy.tv.source.torrent.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.TorrentSort
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
    fun `sort field and reversal are applied`() = runTest {
        coEvery { indexer.search("q") } returns listOf(
            TorrentResult("b", sizeBytes = 100, seeders = 1, peers = 0, tracker = "t", link = "1"),
            TorrentResult("A", sizeBytes = 300, seeders = 5, peers = 0, tracker = "t", link = "2"),
            TorrentResult("c", sizeBytes = 200, seeders = 9, peers = 0, tracker = "t", link = "3"),
        )
        val useCase = createUseCase()

        assertEquals(listOf("3", "2", "1"), useCase(listOf("q"), TorrentSort.SEEDERS, false).map { it.link })
        assertEquals(listOf("1", "2", "3"), useCase(listOf("q"), TorrentSort.SEEDERS, true).map { it.link })
        assertEquals(listOf("2", "3", "1"), useCase(listOf("q"), TorrentSort.SIZE, false).map { it.link })
        assertEquals(listOf("2", "1", "3"), useCase(listOf("q"), TorrentSort.TITLE, false).map { it.link })
        assertEquals(listOf("3", "1", "2"), useCase(listOf("q"), TorrentSort.TITLE, true).map { it.link })
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

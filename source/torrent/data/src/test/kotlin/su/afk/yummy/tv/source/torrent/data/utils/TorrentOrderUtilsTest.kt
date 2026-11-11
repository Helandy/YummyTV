package su.afk.yummy.tv.source.torrent.data.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult

class TorrentOrderUtilsTest : BaseUnitTest() {

    private fun result(title: String, hash: String?) = TorrentResult(
        title = title,
        sizeBytes = 0,
        seeders = 0,
        peers = 0,
        tracker = "",
        link = if (hash == null) "http://x/file.torrent" else "magnet:?xt=urn:btih:$hash",
    )

    private val a = result("a", "a".repeat(40))
    private val b = result("b", "b".repeat(40))
    private val c = result("c", "c".repeat(40))
    private val d = result("d", null)

    @Test
    fun `current first then known then rest keeping indexer order`() {
        val sorted = listOf(a, b, c, d).pinServerTorrentsFirst(
            currentHash = "c".repeat(40),
            knownHashes = setOf("b".repeat(40), "c".repeat(40)),
        )

        assertEquals(listOf(c, b, a, d), sorted)
    }

    @Test
    fun `order is unchanged when nothing is on the server`() {
        assertEquals(listOf(a, b, c, d), listOf(a, b, c, d).pinServerTorrentsFirst(null, emptySet()))
    }
}

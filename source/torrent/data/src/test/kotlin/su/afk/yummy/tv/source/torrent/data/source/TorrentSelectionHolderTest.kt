package su.afk.yummy.tv.source.torrent.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection

class TorrentSelectionHolderTest : BaseUnitTest() {

    private val selection = TorrentSelection("h", "t", TorrentFile(1, "a.mkv", 1))
    private val holder = TorrentSelectionHolder()

    @Test
    fun `selection is returned for same url within window`() {
        holder.put("u", selection, nowMs = 0)

        assertEquals(selection, holder.get("u", nowMs = 60_000))
    }

    @Test
    fun `selection is dropped for another url or after window`() {
        holder.put("u", selection, nowMs = 0)

        assertNull(holder.get("other", nowMs = 1))
        assertNull(holder.get("u", nowMs = 16 * 60_000L))
    }

    @Test
    fun `remembered torrent is returned for the same anime within window`() {
        val torrent = TorrentResult("t", 1, 1, 1, "tr", "magnet:?xt=urn:btih:h")
        holder.rememberTorrent(7, torrent, nowMs = 0)

        assertEquals(torrent, holder.torrentFor(7, nowMs = 60_000))
        assertNull(holder.torrentFor(8, nowMs = 60_000))
        assertNull(holder.torrentFor(7, nowMs = 16 * 60_000L))
    }

    @Test
    fun `started selection is reusable only for a short restart window`() {
        holder.put("u", selection, nowMs = 0)

        holder.markStarted("u", nowMs = 1_000)

        assertEquals(selection, holder.get("u", nowMs = 20_000))
        assertNull(holder.get("u", nowMs = 40_000))
    }
}

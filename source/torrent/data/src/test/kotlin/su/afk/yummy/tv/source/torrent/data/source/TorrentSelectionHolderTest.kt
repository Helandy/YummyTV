package su.afk.yummy.tv.source.torrent.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
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
}

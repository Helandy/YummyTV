package su.afk.yummy.tv.source.torrent.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class TorrentFileExtensionsTest : BaseUnitTest() {

    private fun file(path: String) = TorrentFile(index = 1, path = path, sizeBytes = 1)

    @Test
    fun `video detection uses extension case-insensitively`() {
        assertTrue(file("Show/ep01.MKV").isVideo)
        assertTrue(file("a.mp4").isVideo)
        assertFalse(file("Show/ep01.ass").isVideo)
        assertFalse(file("poster.jpg").isVideo)
        assertFalse(file("noextension").isVideo)
    }

    @Test
    fun `file name drops torrent directories`() {
        assertEquals("ep01.mkv", file("Show [BD]/Season 1/ep01.mkv").fileName)
    }

    @Test
    fun `episode number matches as standalone number`() {
        assertEquals(1, file("[SubsPlease] Show - 05 (1080p).mkv").episodeMatchScore("5"))
        assertEquals(1, file("Show.S01E05.mkv").episodeMatchScore("5"))
        assertEquals(0, file("[SubsPlease] Show - 06 (1080p).mkv").episodeMatchScore("5"))
    }

    @Test
    fun `resolution and year are not episode numbers`() {
        assertEquals(0, file("Show 1080p 2024.mkv").episodeMatchScore("1080"))
        assertEquals(0, file("Show 1080p 2024.mkv").episodeMatchScore("2024"))
    }

    @Test
    fun `non numeric episode never matches`() {
        assertEquals(0, file("Show - 01.mkv").episodeMatchScore("special"))
    }

    @Test
    fun `single video file is selected automatically`() {
        val files = listOf(file("Show/ep.mkv"), file("Show/subs.ass"))

        assertEquals("Show/ep.mkv", files.autoSelectFor("7")?.path)
    }

    @Test
    fun `episode is selected when exactly one file matches its number`() {
        val files = listOf(
            file("[G] Show - 01 [1080p].mkv"),
            file("[G] Show - 02 [1080p].mkv"),
            file("[G] Show - 03 [1080p].mkv"),
        )

        assertEquals("[G] Show - 02 [1080p].mkv", files.autoSelectFor("2")?.path)
    }

    @Test
    fun `no auto selection when several files match or none`() {
        val sameNumber = listOf(file("Show 2nd Season - 01.mkv"), file("Show 2nd Season - 02.mkv"))
        val noMatch = listOf(file("Show - 01.mkv"), file("Show - 02.mkv"))

        assertNull(sameNumber.autoSelectFor("2"))
        assertNull(noMatch.autoSelectFor("9"))
        assertNull(emptyList<TorrentFile>().autoSelectFor("1"))
    }
}

package su.afk.yummy.tv.source.torrent.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class TorrentTagsTest : BaseUnitTest() {

    @Test
    fun `erai-raws style title`() {
        val tags = parseTorrentTags("[Erai-raws] Black Clover 2nd Season - 04 [1080p CR WEB-DL AVC AAC][MultiSub][564EC459]")

        assertEquals("1080p", tags.resolution)
        assertEquals("WEB-DL", tags.source)
        assertEquals("AVC", tags.codec)
        assertTrue(tags.hasSubtitles)
        assertEquals(listOf("1080p", "WEB-DL", "AVC", "Sub"), tags.toBadges("Sub"))
    }

    @Test
    fun `hevc webrip 720p without subs`() {
        val tags = parseTorrentTags("Чёрный клевер 2 / Black Clover (2026) WEBRip 720p HEVC")

        assertEquals("720p", tags.resolution)
        assertEquals("WEBRip", tags.source)
        assertEquals("HEVC", tags.codec)
        assertFalse(tags.hasSubtitles)
    }

    @Test
    fun `4k and bdremux`() {
        val tags = parseTorrentTags("Show 2160p BDRemux x265")

        assertEquals("4K", tags.resolution)
        assertEquals("BD Remux", tags.source)
        assertEquals("HEVC", tags.codec)
    }

    @Test
    fun `year and episode numbers are not resolution`() {
        val tags = parseTorrentTags("Show (2026, TV, 6 из 12 эп.) 1080 серий")

        assertNull(tags.resolution)
        assertNull(tags.source)
        assertNull(tags.codec)
        assertEquals(emptyList<String>(), tags.toBadges("Sub"))
    }

    @Test
    fun `russian subtitles word is detected, substring of another word is not`() {
        assertTrue(parseTorrentTags("Show [рус. субтитры]").hasSubtitles)
        assertFalse(parseTorrentTags("Substance of dreams 1080p").hasSubtitles)
    }
}

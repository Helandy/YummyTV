package su.afk.yummy.tv.source.anistar.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnistarPlaylistUtilsTest {
    private val html = requireNotNull(javaClass.getResourceAsStream("/anistar-playlist.html")).readBytes().decodeToString()

    @Test
    fun `episodes are read with hls links by quality`() {
        val entries = parseAnistarPlaylist(html)

        val first = entries.first()
        assertEquals(391, first.number)
        assertEquals(
            mapOf(
                360 to "https://sf2.an-media.org/video/9be794f4cf98cc717137ba914067f7b2/360.mp4/index.m3u8",
                720 to "https://sf2.an-media.org/video/9be794f4cf98cc717137ba914067f7b2/720.mp4/index.m3u8",
            ),
            first.hlsByQuality,
        )
        assertEquals(listOf(391, 392, 393), entries.take(3).map { it.number })
    }

    @Test
    fun `mp4 links are not taken and an entry without hls is skipped`() {
        val entries = parseAnistarPlaylist(html)

        assertTrue(entries.flatMap { it.hlsByQuality.values }.all { it.endsWith("/index.m3u8") })
        assertTrue(entries.none { it.number == 9 })
    }

    @Test
    fun `entry without a number gets its ordinal`() {
        val movie = parseAnistarPlaylist(html).first { it.hlsByQuality.keys == setOf(720) }

        assertEquals(4, movie.number)
    }

    @Test
    fun `token hls5 link in files is replaced by the m3u8 link from files_as`() {
        val page = """
            var playlst=[
            { title:"Серия 1", media_id:"1",
              files:[{ title:"360", file:"https://sf2.an-media.org/video/x/360.mp4/index.m3u8" },
                     { title:"720", file:"https://sfv.an-media.org/key=k,end=1/referer=/media=hls5/video/x/720.mp4" }],
              files_mp4:[{ title:"720", file:"https://sfhd.an-media.org/t/1/x/720.mp4" }],
              files_as:[{ title:"360", file:"https://sfhd.an-media.org/t/1/x/360.mp4" },
                        { title:"720", file:"https://sf2.an-media.org/video/x/720.mp4/index.m3u8" }],
            },
            { title:"Серия 2", media_id:"2",
              files:[{ title:"720", file:"https://sfv.an-media.org/key=k,end=1/referer=/media=hls5/video/y/720.mp4" }],
              files_as:[{ title:"360", file:"https://sfhd.an-media.org/t/1/y/360.mp4" }],
            }];
        """.trimIndent()

        val entries = parseAnistarPlaylist(page)

        assertEquals(
            mapOf(360 to "https://sf2.an-media.org/video/x/360.mp4/index.m3u8", 720 to "https://sf2.an-media.org/video/x/720.mp4/index.m3u8"),
            entries[0].hlsByQuality,
        )
        // Другой ссылки нет: берётся токенная, плейлист по ней отдаётся, но без расширения.
        assertEquals(mapOf(720 to "https://sfv.an-media.org/key=k,end=1/referer=/media=hls5/video/y/720.mp4"), entries[1].hlsByQuality)
    }

    @Test
    fun `page without a playlist has no episodes`() {
        assertTrue(parseAnistarPlaylist("<html><body>новость не найдена</body></html>").isEmpty())
    }
}

package su.afk.yummy.tv.source.torrent.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.torrent.data.dto.JackettResponseDto
import su.afk.yummy.tv.source.torrent.data.dto.JacRedTorrentDto
import su.afk.yummy.tv.source.torrent.data.dto.TorrServerTorrentDto
import su.afk.yummy.tv.source.torrent.data.utils.torrentJson
import kotlinx.serialization.decodeFromString

class TorrentMapperTest : BaseUnitTest() {

    @Test
    fun `jacred prefers magnet, falls back to url and drops linkless rows`() {
        val json = """
            [
              {"tracker":"rutor","title":"A","sid":12,"pir":3,"size":1000,"magnet":"magnet:?xt=urn:btih:aaa","extra":1},
              {"tracker":"x","title":"B","sid":1,"url":"http://t/b.torrent"},
              {"title":"no link"}
            ]
        """.trimIndent()

        val results = torrentJson.decodeFromString<List<JacRedTorrentDto>>(json).toJacRedDomain()

        assertEquals(listOf("magnet:?xt=urn:btih:aaa", "http://t/b.torrent"), results.map { it.link })
        assertEquals(12, results[0].seeders)
        assertEquals(1000L, results[0].sizeBytes)
    }

    @Test
    fun `jackett uses magnet uri then link`() {
        val json = """{"Results":[{"Tracker":"t","Title":"A","Size":5,"Seeders":7,"Peers":2,"MagnetUri":null,"Link":"http://l"}],"Indexers":[]}"""

        val result = torrentJson.decodeFromString<JackettResponseDto>(json).toDomain().single()

        assertEquals("http://l", result.link)
        assertEquals(7, result.seeders)
    }

    @Test
    fun `torrserver response maps files and requires hash`() {
        val json = """{"title":"T","hash":"abc","file_stats":[{"id":1,"path":"d/a.mkv","length":10}]}"""

        val content = torrentJson.decodeFromString<TorrServerTorrentDto>(json).toDomain()

        assertEquals("abc", content?.hash)
        assertEquals(1, content?.files?.single()?.index)
        assertEquals(null, torrentJson.decodeFromString<TorrServerTorrentDto>("""{"title":"T"}""").toDomain())
    }
}
